package Services.joboffers;

import java.util.*;

/**
 * Service de recherche floue (Fuzzy Search)
 * Permet de trouver des résultats même avec des fautes de frappe
 * 100% local, sans API externe
 *
 * Utilise l'algorithme de distance de Levenshtein
 */
public class FuzzySearchService {

    private static FuzzySearchService instance;

    public static FuzzySearchService getInstance() {
        if (instance == null) {
            instance = new FuzzySearchService();
        }
        return instance;
    }

    /**
     * Résultat de recherche avec score de pertinence
     */
    public static class SearchResult<T> {
        private T item;
        private double score; // 0.0 à 1.0 (1.0 = correspondance parfaite)
        private String matchedField;
        private String highlightedText;

        public SearchResult(T item, double score, String matchedField, String highlightedText) {
            this.item = item;
            this.score = score;
            this.matchedField = matchedField;
            this.highlightedText = highlightedText;
        }

        public T getItem() { return item; }
        public double getScore() { return score; }
        public String getMatchedField() { return matchedField; }
        public String getHighlightedText() { return highlightedText; }
    }

    /**
     * Calcule la distance de Levenshtein entre deux chaînes
     * (nombre minimum d'opérations pour transformer une chaîne en une autre)
     */
    public int levenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) return Integer.MAX_VALUE;

        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] prev = new int[s2.length() + 1];
        int[] curr = new int[s2.length() + 1];

        for (int j = 0; j <= s2.length(); j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }

        return prev[s2.length()];
    }

    /**
     * Calcule un score de similarité entre 0 et 1
     */
    public double similarityScore(String s1, String s2) {
        if (s1 == null || s2 == null) return 0;
        if (s1.isEmpty() && s2.isEmpty()) return 1;
        if (s1.isEmpty() || s2.isEmpty()) return 0;

        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());

        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Vérifie si une chaîne contient un terme (recherche floue)
     */
    public boolean fuzzyContains(String text, String query, double threshold) {
        if (text == null || query == null) return false;

        text = text.toLowerCase();
        query = query.toLowerCase();

        // Correspondance exacte
        if (text.contains(query)) return true;

        // Recherche dans chaque mot
        String[] words = text.split("\\s+");
        for (String word : words) {
            if (similarityScore(word, query) >= threshold) {
                return true;
            }
        }

        // Recherche par sous-chaînes glissantes
        if (query.length() <= text.length()) {
            for (int i = 0; i <= text.length() - query.length(); i++) {
                String substring = text.substring(i, i + query.length());
                if (similarityScore(substring, query) >= threshold) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Recherche floue dans une liste de textes
     * @return Liste triée par pertinence
     */
    public List<String> fuzzySearch(List<String> items, String query, double threshold) {
        if (query == null || query.trim().isEmpty()) return new ArrayList<>(items);

        List<Map.Entry<String, Double>> scored = new ArrayList<>();

        for (String item : items) {
            double score = calculateBestScore(item, query);
            if (score >= threshold) {
                scored.add(new AbstractMap.SimpleEntry<>(item, score));
            }
        }

        // Trier par score décroissant
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<String> results = new ArrayList<>();
        for (Map.Entry<String, Double> entry : scored) {
            results.add(entry.getKey());
        }

        return results;
    }

    /**
     * Calcule le meilleur score pour un texte par rapport à une requête
     */
    public double calculateBestScore(String text, String query) {
        if (text == null || query == null) return 0;

        text = text.toLowerCase();
        query = query.toLowerCase();

        // Correspondance exacte = score parfait
        if (text.equals(query)) return 1.0;

        // Contient la requête = très bon score
        if (text.contains(query)) return 0.95;

        // Calcul du meilleur score mot par mot
        String[] queryWords = query.split("\\s+");
        String[] textWords = text.split("\\s+");

        double totalScore = 0;
        int matchedWords = 0;

        for (String qWord : queryWords) {
            double bestWordScore = 0;
            for (String tWord : textWords) {
                double wordScore = similarityScore(qWord, tWord);
                bestWordScore = Math.max(bestWordScore, wordScore);
            }
            if (bestWordScore > 0.5) {
                matchedWords++;
                totalScore += bestWordScore;
            }
        }

        if (matchedWords == 0) return 0;

        // Score final = moyenne des scores * ratio de mots trouvés
        double avgScore = totalScore / queryWords.length;
        double coverage = (double) matchedWords / queryWords.length;

        return avgScore * coverage;
    }

    /**
     * Suggestions de correction pour une faute de frappe
     */
    public List<String> getSuggestions(String query, List<String> dictionary, int maxSuggestions) {
        if (query == null || query.isEmpty()) return new ArrayList<>();

        List<Map.Entry<String, Double>> scored = new ArrayList<>();

        for (String word : dictionary) {
            double score = similarityScore(query, word);
            if (score > 0.5) {
                scored.add(new AbstractMap.SimpleEntry<>(word, score));
            }
        }

        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<String> suggestions = new ArrayList<>();
        for (int i = 0; i < Math.min(maxSuggestions, scored.size()); i++) {
            suggestions.add(scored.get(i).getKey());
        }

        return suggestions;
    }

    /**
     * Met en surbrillance les parties correspondantes
     */
    public String highlightMatch(String text, String query) {
        if (text == null || query == null) return text;

        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();

        int index = lowerText.indexOf(lowerQuery);
        if (index >= 0) {
            return text.substring(0, index) +
                   "【" + text.substring(index, index + query.length()) + "】" +
                   text.substring(index + query.length());
        }

        return text;
    }

    /**
     * Recherche floue multi-critères
     */
    public boolean matchesAny(String query, double threshold, String... fields) {
        for (String field : fields) {
            if (field != null && fuzzyContains(field, query, threshold)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normalise un texte pour la recherche
     * (supprime accents, ponctuation, etc.)
     */
    public String normalize(String text) {
        if (text == null) return "";

        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}

