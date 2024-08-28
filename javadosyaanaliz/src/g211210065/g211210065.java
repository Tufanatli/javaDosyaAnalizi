import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            String repoUrl = getGitHubRepoUrl();
            if (cloneRepository(repoUrl)) {
                File repoDirectory = new File(System.getProperty("user.dir") + "/" + getRepoName(repoUrl));
                List<File> javaFiles = findJavaFiles(repoDirectory);
                for (File javaFile : javaFiles) {
                    analyzeJavaFile(javaFile);
                }
            } else {
                System.err.println("Hata: Depo klonlanamadı.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getGitHubRepoUrl() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Lütfen GitHub depo linkini giriniz:");
        return reader.readLine();
    }

    public static boolean cloneRepository(String repoUrl) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("git", "clone", repoUrl);
            processBuilder.directory(new File(System.getProperty("user.dir")));
            Process process = processBuilder.start();
            process.waitFor();
            return true;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getRepoName(String repoUrl) {
        int lastSlashIndex = repoUrl.lastIndexOf('/');
        int lastDotIndex = repoUrl.lastIndexOf('.');
        
        if (lastDotIndex > lastSlashIndex) {
            return repoUrl.substring(lastSlashIndex + 1, lastDotIndex);
        } else {
            return repoUrl.substring(lastSlashIndex + 1);
        }
    }


    public static List<File> findJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        for (File file : directory.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".java")) {
                javaFiles.add(file);
            } else if (file.isDirectory()) {
                javaFiles.addAll(findJavaFiles(file));
            }
        }
        return javaFiles;
    }

    public static void analyzeJavaFile(File javaFile) {
        try {
            if (hasClass(javaFile)) {
                int[] stats = analyze(javaFile);
                printAnalysis(javaFile, stats);
            } else {
                System.out.println("Hata: Sınıf bulunmayan dosya: " + javaFile.getName());
                System.out.println("--------------------------------------------");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean hasClass(File javaFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(javaFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("class ")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int[] analyze(File javaFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(javaFile))) {
            String line;
            int[] stats = new int[5]; // 0: Javadoc, 1: Comment, 2: Code, 3: Function, 4: LOC
            boolean inJavadoc = false;
            boolean inFunction = false;
            boolean inMultilineComment = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Javadoc sayma
                if (line.startsWith("/**") && !inMultilineComment) {
                    inJavadoc = true;
                } else if (inJavadoc && line.endsWith("*/")) {
                    inJavadoc = false;
                } else if (inJavadoc) {
                    stats[0]++;
                }

                // Yorum satırı sayma
                if (line.contains("//")) {
                    stats[1]++;
                }
                
                // Çoklu yorum satırı sayma
                if (line.startsWith("/*") && !inJavadoc) {
                    inMultilineComment = true;
                } else if (inMultilineComment && line.endsWith("*/")) {
                    inMultilineComment = false;
                } else if (inMultilineComment) {
                    stats[1]++;
                }

                if (!line.trim().isEmpty() && !line.startsWith("//") && !inJavadoc && !line.endsWith("*/")) {
                    stats[2]++;
                }

                // Fonksiyon sayma
                if (!inFunction && line.matches(".*\\{\\s*$") && !inMultilineComment) {
                    stats[3]++;
                    inFunction = true;
                }

                // Fonksiyonun bitişini kontrol etme
                if (inFunction && line.matches("^\\s*\\}\\s*$") && !inMultilineComment) {
                    inFunction = false;
                }

                stats[4]++;
            }
            
            return stats;
        }
    }

    public static void printAnalysis(File javaFile, int[] stats) {
        String className = javaFile.getName().substring(0, javaFile.getName().lastIndexOf('.'));
        double commentDeviationPercentage = calculateCommentDeviationPercentage(stats);
        System.out.println("Sınıf: " + className);
        System.out.println("Javadoc Satır Sayısı: " + stats[0]);
        System.out.println("Yorum Satır Sayısı: " + (stats[1]));
        System.out.println("Kod Satır Sayısı: " + stats[2]);
        System.out.println("Fonksiyon Sayısı: " + stats[3]);
        System.out.println("LOC: " + stats[4]);
        System.out.printf("Yorum Sapma Yüzdesi: %.2f %%\n", commentDeviationPercentage);
        System.out.println("--------------------------------------------");
    }

    public static double calculateCommentDeviationPercentage(int[] stats) {
        int totalCommentLines = stats[0] + stats[1];
        int totalCodeLines = stats[2];
        int functionCount = stats[3];
        double YG = ((totalCommentLines) * 0.8) / functionCount;
        double YH = ((totalCodeLines) * 0.3) / functionCount;
        return ((100 * YG) / YH) - 100;
    }
}
