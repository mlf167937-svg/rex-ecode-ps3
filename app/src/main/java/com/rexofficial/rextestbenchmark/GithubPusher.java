// com/rexofficial/rextestbenchmark/GithubPusher.java
package com.rexofficial.rextestbenchmark;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class GithubPusher {

    private static final String README_CONTENT = "# RexApsTestBenchmark\n\n"
            + "Repo ini dibuat otomatis oleh aplikasi RexTestBenchmark.\n\n"
            + "PERINGATAN: Alat ini digunakan untuk pengujian beban (benchmark) terhadap server yang Anda miliki atau yang Anda punya izin tertulis untuk diuji. "
            + "Penggunaan terhadap target tanpa izin adalah tindakan ilegal dan sepenuhnya menjadi tanggung jawab pengguna. "
            + "Pemilik aplikasi dan pembuat repo tidak bertanggung jawab atas penyalahgunaan.\n";

    private static final String WORKFLOW_CONTENT = "name: RexBenchmark\n"
            + "on:\n"
            + "  workflow_dispatch:\n"
            + "    inputs:\n"
            + "      target:\n"
            + "        description: Target URL\n"
            + "        required: true\n"
            + "      threads:\n"
            + "        description: Threads\n"
            + "        required: true\n"
            + "        default: '10'\n"
            + "      connections:\n"
            + "        description: Connections\n"
            + "        required: true\n"
            + "        default: '100'\n"
            + "      duration:\n"
            + "        description: Duration seconds\n"
            + "        required: true\n"
            + "        default: '30'\n"
            + "jobs:\n"
            + "  bench:\n"
            + "    runs-on: ubuntu-latest\n"
            + "    steps:\n"
            + "      - uses: actions/checkout@v4\n"
            + "      - name: Install wrk\n"
            + "        run: sudo apt-get update && sudo apt-get install -y wrk\n"
            + "      - name: Run benchmark\n"
            + "        run: |\n"
            + "          echo \"target=${{ inputs.target }}\" > result.txt\n"
            + "          echo \"threads=${{ inputs.threads }}\" >> result.txt\n"
            + "          echo \"connections=${{ inputs.connections }}\" >> result.txt\n"
            + "          echo \"duration=${{ inputs.duration }}\" >> result.txt\n"
            + "          echo \"---\" >> result.txt\n"
            + "          wrk -t${{ inputs.threads }} -c${{ inputs.connections }} -d${{ inputs.duration }}s --latency ${{ inputs.target }} >> result.txt 2>&1 || true\n"
            + "      - name: Upload artifact\n"
            + "        uses: actions/upload-artifact@v4\n"
            + "        with:\n"
            + "          name: benchmark-result\n"
            + "          path: result.txt\n";

    public static boolean pushFiles(String token, String owner, String repoName) {
        File workDir = new File(System.getProperty("java.io.tmpdir"), "rex_bench_" + System.currentTimeMillis());
        try {
            workDir.mkdirs();
            File readme = new File(workDir, "README.md");
            writeFile(readme, README_CONTENT);
            File wfDir = new File(workDir, ".github/workflows");
            wfDir.mkdirs();
            File wf = new File(wfDir, "benchmark.yml");
            writeFile(wf, WORKFLOW_CONTENT);

            String remote = "https://github.com/" + owner + "/" + repoName + ".git";
            Git git;
            try {
                git = Git.cloneRepository()
                        .setURI(remote)
                        .setDirectory(workDir)
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                        .setCloneAllBranches(true)
                        .call();
            } catch (Exception cloneEx) {
                git = Git.init().setDirectory(workDir).call();
                git.remoteAdd().setName("origin").setUri(new org.eclipse.jgit.transport.URIish(remote)).call();
            }

            File readmeDest = new File(workDir, "README.md");
            writeFile(readmeDest, README_CONTENT);
            File wfDestDir = new File(workDir, ".github/workflows");
            wfDestDir.mkdirs();
            writeFile(new File(wfDestDir, "benchmark.yml"), WORKFLOW_CONTENT);

            git.add().addFilepattern(".").call();
            git.add().addFilepattern(".github/workflows/benchmark.yml").call();
            git.commit()
                    .setMessage("RexTestBenchmark: init workflow")
                    .setAuthor("RexTestBenchmark", "rex@local")
                    .setCommitter("RexTestBenchmark", "rex@local")
                    .call();

            git.push()
                    .setRemote("origin")
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                    .call();
            git.close();
            return true;
        } catch (GitAPIException | java.io.IOException | java.net.URISyntaxException e) {
            return false;
        } finally {
            deleteDir(workDir);
        }
    }

    private static void writeFile(File f, String content) throws java.io.IOException {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) deleteDir(f);
        }
        dir.delete();
    }
}