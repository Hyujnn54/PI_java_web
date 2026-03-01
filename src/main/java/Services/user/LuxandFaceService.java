package Services.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String APP_COLLECTION = "TalentBridgeApp";

    // ── Public API ────────────────────────────────────────────────────────────

    public String addPerson(String name, byte[] photoBytes, String fileName) throws Exception {
        String url = LuxandConfig.BASE + "/v2/person";

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("name", name);
        fields.put("store", "1");
        fields.put("collections", APP_COLLECTION);

        HttpRequest req = multipartRequest(url, fields, "photos", fileName, photoBytes);

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("ADDPERSON status=" + res.statusCode());
        System.out.println("ADDPERSON body=" + res.body());

        if (res.statusCode() >= 400)
            throw new RuntimeException("Luxand addPerson failed: " + res.body());

        JsonNode root = mapper.readTree(res.body());
        JsonNode uuidNode = root.get("uuid");
        if (uuidNode == null || uuidNode.isNull())
            throw new RuntimeException("Luxand addPerson: uuid missing. Response=" + res.body());

        return uuidNode.asText();
    }

    public void addFace(String personUuid, byte[] photoBytes, String fileName) throws Exception {
        if (personUuid == null || personUuid.isBlank())
            throw new IllegalArgumentException("personUuid is null/blank");

        String url = LuxandConfig.BASE + "/person/" + personUuid;

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("store", "1");
        fields.put("collections", APP_COLLECTION);

        HttpRequest req = multipartRequest(url, fields, "photo", fileName, photoBytes);
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        System.out.println("ADDFACE url=" + url);
        System.out.println("ADDFACE status=" + res.statusCode());
        System.out.println("ADDFACE body=" + res.body());

        if (res.statusCode() >= 400)
            throw new RuntimeException("Luxand addFace failed: " + res.body());
    }

    public FaceMatch searchBestMatch(byte[] imageBytes, String fileName) throws Exception {
        String url = LuxandConfig.BASE + "/photo/search/v2";

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("collections", APP_COLLECTION);

        HttpRequest req = multipartRequest(url, fields, "photo", fileName, imageBytes);
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("SEARCH status = " + res.statusCode());
        System.out.println("SEARCH body = " + res.body());

        if (res.statusCode() >= 400)
            throw new RuntimeException("Search failed: " + res.body());

        JsonNode root = mapper.readTree(res.body());
        if (!root.isArray() || root.isEmpty()) return null;

        JsonNode first = root.get(0);
        if (!first.has("uuid")) return null;

        FaceMatch match = new FaceMatch();
        match.uuid = first.get("uuid").asText();
        match.probability = first.get("probability").asDouble();
        return match;
    }

    public LivenessResult isLive(byte[] imageBytes, String fileName) throws Exception {
        String url = LuxandConfig.BASE + "/photo/liveness";

        Map<String, String> fields = new HashMap<>();
        HttpRequest request = multipartRequest(url, fields, "photo", fileName, imageBytes);
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("LIVENESS status = " + response.statusCode());
        System.out.println("LIVENESS body   = " + response.body());

        if (response.statusCode() != 200)
            throw new RuntimeException("API Error: " + response.body());

        JsonNode root = mapper.readTree(response.body());
        boolean isReal = "real".equalsIgnoreCase(root.path("result").asText());
        double score = root.path("score").asDouble();
        JsonNode rect = root.path("rectangle");

        return new LivenessResult(
                isReal, score,
                rect.path("left").asInt(),
                rect.path("top").asInt(),
                rect.path("right").asInt(),
                rect.path("bottom").asInt()
        );
    }

    public List<String> listPersonUuids() throws Exception {
        URL url = new URL(LuxandConfig.BASE + "/v2/person");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("token", LuxandConfig.API_TOKEN);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(20_000);

        int status = conn.getResponseCode();
        InputStream listIs = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
        String body = listIs == null ? "" : new String(listIs.readAllBytes(), StandardCharsets.UTF_8);
        System.out.println("LIST status = " + status);
        System.out.println("LIST body = " + body);

        if (status >= 400)
            throw new RuntimeException("List persons failed: " + body);

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
        int code1 = deleteByUrl(LuxandConfig.BASE + "/v2/person/" + uuid.trim());
        if (code1 == 200) return;
        deleteByUrl(LuxandConfig.BASE + "/person/" + uuid.trim());
    }

    private int deleteByUrl(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("token", LuxandConfig.API_TOKEN);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(15_000);
        int status = conn.getResponseCode();
        System.out.println("DELETE url = " + urlStr);
        System.out.println("DELETE status = " + status);
        return status;
    }

    public String registerNewPerson(String name, List<byte[]> photoList) throws Exception {
        if (photoList == null || photoList.isEmpty()) throw new Exception("No photos captured.");
        String personUuid = addPerson(name, photoList.get(0), "face_0.jpg");
        for (int i = 1; i < photoList.size(); i++)
            addFace(personUuid, photoList.get(i), "face_" + i + ".jpg");
        return personUuid;
    }

    private HttpRequest multipartRequest(String url, Map<String, String> fields,
                                          String fileField, String fileName, byte[] fileBytes) {
        String boundary = "----LuxandBoundary" + UUID.randomUUID();
        byte[] body = buildMultipartBody(boundary, fields, fileField, fileName, fileBytes);

        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("token", LuxandConfig.API_TOKEN)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
    }

    private byte[] buildMultipartBody(String boundary, Map<String, String> fields,
                                       String fileField, String fileName, byte[] fileBytes) {
        List<byte[]> parts = new ArrayList<>();

        for (var e : fields.entrySet()) {
            parts.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            parts.add(("Content-Disposition: form-data; name=\"" + e.getKey() + "\"\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            parts.add((e.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }

        String mime = "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) mime = "image/jpeg";
        else if (lower.endsWith(".png")) mime = "image/png";

        parts.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        parts.add(("Content-Disposition: form-data; name=\"" + fileField
                + "\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        parts.add(("Content-Type: " + mime + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        parts.add(fileBytes);
        parts.add("\r\n".getBytes(StandardCharsets.UTF_8));
        parts.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        int len = parts.stream().mapToInt(p -> p.length).sum();
        byte[] out = new byte[len];
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
        public final double score;
        public final int left, top, right, bottom;

        public LivenessResult(boolean isReal, double score,
                              int left, int top, int right, int bottom) {
            this.isReal = isReal; this.score = score;
            this.left = left; this.top = top; this.right = right; this.bottom = bottom;
        }
    }
}

