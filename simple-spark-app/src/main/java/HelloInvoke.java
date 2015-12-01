/* SimpleApp.java */
import org.apache.spark.api.java.*;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.Function;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class HelloInvoke {
  private native byte[] invokeHS(byte[] clos, int closLen, byte[] arg, int argLen);
  private native void   hask_init();
  private native void   hask_end();

  static {
      System.loadLibrary("HelloInvoke");
  }

  public static void main(String[] args) {
    String logFile = "./README.md"; // Should be some file on your system
    SparkConf conf = new SparkConf().setAppName("Hello hs-invoke!");
    JavaSparkContext sc = new JavaSparkContext(conf);
    JavaRDD<String> logData = sc.textFile(logFile).cache();

    long numAs = logData.filter(new Function<String, Boolean>() {
      public Boolean call(String s) { return s.contains("a"); }
    }).count();

    long numBs = logData.filter(new Function<String, Boolean>() {
      public Boolean call(String s) {
        Path resultPath = FileSystems.getDefault().getPath("./result.bin");

        try {
            // our serialized function, f x = x * 2, as an
            // array of bytes
            byte[] clos =
              { 0, 22, -98, -13
              , -40, 92, -87, 116
              , -56, -112, 123, -68
              , 126, -43, 43, 32
              , -61
              };

            // our serialized argument, 20
            byte[] arg = { 0, 0, 0, 0, 0, 0, 0, 20 };

            System.out.println("About to call Haskell");
            HelloInvoke hs = new HelloInvoke();
            System.out.println("Starting Haskell RTS...");
            hs.hask_init();
            System.out.println("Haskell RTS started");
            byte[] res = hs.invokeHS(clos, clos.length, arg, arg.length);
            System.out.println("Shutting down Haskell RTS...");
            hs.hask_end();
            System.out.println("Came back from Haskell and RTS is down");
            Files.write(resultPath, res);

        } catch (IOException e) {
            System.out.println(e);
        }
        return s.contains("b");
      }
    }).count();

    System.out.println("Lines with a: " + numAs + ", lines with b: " + numBs);
  }
}
