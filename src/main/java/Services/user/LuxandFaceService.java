package Services.user;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LuxandFaceService {

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private static final String APP_COLLECTION = "TalentBridgeApp";

    // ---------- PUBLIC API ----------

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

        JSONObject root = new JSONObject(res.body());
        if (!root.has("uuid") || root.isNull("uuid"))
            throw new RuntimeException("Luxand addPerson: uuid missing. Response=" + res.body());

        return root.getString("uuid");
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

        JSONArray arr = new JSONArray(res.body());
        if (arr.isEmpty()) return null;

        JSONObject first = arr.getJSONObject(0);
        if (!first.has("uuid")) return null;

        FaceMatch match = new FaceMatch();
        match.uuid = first.getString("uuid");
        match.probability = first.optDouble("probability", 0.0);
        return match;
    }

    public List<String> listPersonUuids() throws Exception {
        String url = LuxandConfig.BASE + "/v2/person";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("token", LuxandConfig.API_TOKEN)
                .GET()
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("LIST status = " + res.statusCode());
        System.out.println("LIST body = " + res.body());

        if (res.statusCode() >= 400)
            throw new RuntimeException("List persons failed: " + res.body());

        List<String> uuids = new ArrayList<>();
        String body = res.body().trim();

        if (body.startsWith("[")) {
            JSONArray arr = new JSONArray(body);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject p = arr.optJSONObject(i);
                if (p != null && p.has("uuid")) uuids.add(p.getString("uuid"));
            }
        } else {
            JSONObject root = new JSONObject(body);
            if (root.has("persons")) {
                JSONArray persons = root.getJSONArray("persons");
                for (int i = 0; i < persons.length(); i++) {
                    JSONObject p = persons.optJSONObject(i);
                    if (p != null && p.has("uuid")) uuids.add(p.getString("uuid"));
                }
            }
        }
        return uuids;
    }

    public LivenessResult isLive(byte[] imageBytes, String fileName) throws Exception {
        String url = LuxandConfig.BASE + "/photo/liveness";

        HttpRequest request = multipartRequest(url, new HashMap<>(), "photo", fileName, imageBytes);
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("LIVENESS status = " + response.statusCode());
        System.out.println("LIVENESS body   = " + response.body());

        if (response.statusCode() != 200)
            throw new RuntimeException("API Error: " + response.body());

        JSONObject root = new JSONObject(response.body());
        boolean isReal = "real".equalsIgnoreCase(root.optString("result", ""));
        double score = root.optDouble("score", 0.0);

        JSONObject rect = root.optJSONObject("rectangle");
        int left = 0, top = 0, right = 0, bottom = 0;
        if (rect != null) {
            left   = rect.optInt("left",   0);
            top    = rect.optInt("top",    0);
            right  = rect.optInt("right",  0);
            bottom = rect.optInt("bottom", 0);
        }
        return new LivenessResult(isReal, score, left, top, right, bottom);
    }

    public String registerNewPerson(String name, List<byte[]> photoList) throws Exception {
        if (photoList == null || photoList.isEmpty()) throw new Exception("No photos captured.");
        String personUuid = addPerson(name, photoList.get(0), "face_0.jpg");
        for (int i = 1; i < photoList.size(); i++)
            addFace(personUuid, photoList.get(i), "face_" + i + ".jpg");
        return personUuid;
    }

    public void deletePerson(String uuid) throws Exception {
        int code1 = deleteByUrl(LuxandConfig.BASE + "/v2/person/" + uuid);
        if (code1 == 200) return;
        deleteByUrl(LuxandConfig.BASE + "/person/" + uuid);
    }

    private int deleteByUrl(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("token", LuxandConfig.API_TOKEN)
                .DELETE()
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("DELETE url = " + url);
        System.out.println("DELETE status = " + res.statusCode());
        return res.statusCode();
    }

    // ---------- MULTIPART HELPER ----------

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
            parts.add(("Content-Disposition: form-data; name=\"" + e.getKey() + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            parts.add((e.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }

        String mime = "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) mime = "image/jpeg";
        else if (lower.endsWith(".png")) mime = "image/png";

        parts.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        parts.add(("Content-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
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

    // ---------- INNER CLASSES ----------

    public static class FaceMatch {
        public String uuid;
        public double probability;
    }

    public static class LivenessResult {
        public final boolean isReal;
        public final double score;
        public final int left, top, right, bottom;

        public LivenessResult(boolean isReal, double score, int left, int top, int right, int bottom) {
            this.isReal = isReal; this.score = score;
            this.left = left; this.top = top; this.right = right; this.bottom = bottom;
        }
    }
}

