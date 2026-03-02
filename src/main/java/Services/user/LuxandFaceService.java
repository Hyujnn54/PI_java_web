package Services.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Luxand Cloud face service.
 *
 * Same endpoints and field names as the working desktop version:
 *   POST   /v2/person        field "photos"  – create person
 *   POST   /person/{uuid}    field "photo"   – add extra face
 *   POST   /photo/search/v2  field "photo"   – search
 *   POST   /photo/liveness   field "photo"   – liveness
 *   DELETE /v2/person/{uuid}                 – delete
 *
 * Uses HttpURLConnection instead of HttpClient to avoid the JDK 17 bug:
 *   "fixed content-length: 37, bytes received: 0"
 * HttpClient reuses SSL connections that the server has already closed.
 * HttpURLConnection opens a fresh connection for every call.
 */
public class LuxandFaceService {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final String APP_COLLECTION = "TalentBridgeApp";

    // ── Public API ────────────────────────────────────────────────────────────

    public String addPerson(String name, byte[] photoBytes, String fileName) throws Exception {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("name",        name);
        fields.put("store",       "1");
        fields.put("collections", APP_COLLECTION);

        String body = post(LuxandConfig.BASE + "/v2/person", fields, "photos", fileName, photoBytes);
        System.out.println("ADDPERSON body=" + body);

        JsonNode root = mapper.readTree(body);
        JsonNode uuidNode = root.get("uuid");
        if (uuidNode == null || uuidNode.isNull())
            throw new RuntimeException("Luxand addPerson: uuid missing. body=" + body);
        return uuidNode.asText();
    }

    public void addFace(String personUuid, byte[] photoBytes, String fileName) throws Exception {
        if (personUuid == null || personUuid.isBlank())
            throw new IllegalArgumentException("personUuid is null/blank");

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("store",       "1");
        fields.put("collections", APP_COLLECTION);

        String body = post(LuxandConfig.BASE + "/person/" + personUuid.trim(),
                fields, "photo", fileName, photoBytes);
        System.out.println("ADDFACE body=" + body);
    }

    public FaceMatch searchBestMatch(byte[] imageBytes, String fileName) throws Exception {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("collections", APP_COLLECTION);

        String body = post(LuxandConfig.BASE + "/photo/search/v2",
                fields, "photo", fileName, imageBytes);
        System.out.println("SEARCH body = " + body);

        JsonNode root = mapper.readTree(body);
        if (!root.isArray() || root.isEmpty()) return null;

        JsonNode first = root.get(0);
        if (!first.has("uuid")) return null;

        FaceMatch m = new FaceMatch();
        m.uuid        = first.get("uuid").asText();
        m.probability = first.get("probability").asDouble();
        return m;
    }

    public LivenessResult isLive(byte[] imageBytes, String fileName) throws Exception {
        String body = post(LuxandConfig.BASE + "/photo/liveness",
                new HashMap<>(), "photo", fileName, imageBytes);
        System.out.println("LIVENESS body   = " + body);

        JsonNode root = mapper.readTree(body);
        boolean isReal = "real".equalsIgnoreCase(root.path("result").asText());
        double  score  = root.path("score").asDouble();
        JsonNode rect  = root.path("rectangle");

        return new LivenessResult(isReal, score,
                rect.path("left").asInt(),  rect.path("top").asInt(),
                rect.path("right").asInt(), rect.path("bottom").asInt());
    }

    public List<String> listPersonUuids() throws Exception {
        URL url = new URL(LuxandConfig.BASE + "/v2/person");
        HttpURLConnection conn = open(url, "GET");
        int status = conn.getResponseCode();
        String body = read(conn);
        System.out.println("LIST body = " + body);

        // Return empty list on quota/auth errors — don't crash the cleanup
        if (status == 401 || status == 429 ||
            (body != null && (body.contains("Requests number per month") || body.contains("Upgrade your plan")))) {
            System.err.println("[Luxand] listPersonUuids skipped: " + body);
            return new ArrayList<>();
        }
        if (status >= 400) throw new RuntimeException("List persons failed: " + body);

        JsonNode root = mapper.readTree(body);
        List<String> uuids = new ArrayList<>();
        if (root.isArray()) {
            for (JsonNode p : root)
                if (p.has("uuid")) uuids.add(p.get("uuid").asText());
            return uuids;
        }
        JsonNode persons = root.get("persons");
        if (persons != null && persons.isArray())
            for (JsonNode p : persons)
                if (p.has("uuid")) uuids.add(p.get("uuid").asText());
        return uuids;
    }

    public void deletePerson(String uuid) throws Exception {
        int code = deleteUrl(LuxandConfig.BASE + "/v2/person/" + uuid.trim());
        if (code == 200) return;
        deleteUrl(LuxandConfig.BASE + "/person/" + uuid.trim());
    }

    private int deleteUrl(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("token", LuxandConfig.API_TOKEN);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(15_000);
        int status = conn.getResponseCode();
        System.out.println("DELETE " + urlStr + " => " + status);
        return status;
    }

    public String registerNewPerson(String name, List<byte[]> photoList) throws Exception {
        if (photoList == null || photoList.isEmpty()) throw new Exception("No photos provided.");
        String personUuid = addPerson(name, photoList.get(0), "face_0.jpg");
        for (int i = 1; i < photoList.size(); i++)
            addFace(personUuid, photoList.get(i), "face_" + i + ".jpg");
        return personUuid;
    }

    private String post(String urlStr, Map<String, String> fields,
                        String fileField, String fileName, byte[] fileBytes) throws Exception {
        String boundary = "----LuxandBoundary" + UUID.randomUUID().toString().replace("-", "");
        byte[] body     = multipart(boundary, fields, fileField, fileName, fileBytes);

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(20_000);
        conn.setReadTimeout(30_000);
        conn.setRequestProperty("token",        LuxandConfig.API_TOKEN);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setFixedLengthStreamingMode(body.length);

        try (OutputStream os = conn.getOutputStream()) { os.write(body); }

        int    status   = conn.getResponseCode();
        String respBody = read(conn);
        System.out.println("POST " + urlStr + " => HTTP " + status);

        if (status == 401 || status == 429 ||
            (respBody != null && (respBody.contains("Requests number per month") || respBody.contains("Upgrade your plan"))))
            throw new RuntimeException("Luxand quota exceeded or plan inactive.\nUpgrade at: dashboard.luxand.cloud/#activate");
        if (status >= 400)
            throw new RuntimeException("Luxand HTTP " + status + ": " + respBody);
        return respBody;
    }

    private HttpURLConnection open(URL url, String method) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("token", LuxandConfig.API_TOKEN);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(20_000);
        return conn;
    }

    private String read(HttpURLConnection conn) throws Exception {
        int status = conn.getResponseCode();
        InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return "";
        try (InputStream s = is) {
            return new String(s.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private byte[] multipart(String boundary, Map<String, String> fields,
                              String fileField, String fileName, byte[] fileBytes) {
        List<byte[]> parts = new ArrayList<>();
        for (Map.Entry<String, String> e : fields.entrySet()) {
            parts.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            parts.add(("Content-Disposition: form-data; name=\"" + e.getKey() + "\"\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            parts.add((e.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        String mime = fileName.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
        parts.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        parts.add(("Content-Disposition: form-data; name=\"" + fileField
                + "\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        parts.add(("Content-Type: " + mime + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        parts.add(fileBytes);
        parts.add("\r\n".getBytes(StandardCharsets.UTF_8));
        parts.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        int total = parts.stream().mapToInt(p -> p.length).sum();
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] p : parts) { System.arraycopy(p, 0, out, pos, p.length); pos += p.length; }
        return out;
    }

    // ── Inner classes ─────────────────────────────────────────────────────────

    public static class FaceMatch {
        public String uuid;
        public double probability;
    }

    public static class LivenessResult {
        public final boolean isReal;
        public final double  score;
        public final int     left, top, right, bottom;

        public LivenessResult(boolean isReal, double score,
                              int left, int top, int right, int bottom) {
            this.isReal = isReal; this.score = score;
            this.left = left; this.top = top; this.right = right; this.bottom = bottom;
        }
    }
}

