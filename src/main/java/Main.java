import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * Runs many simulations of a checkout under the given parameters, storing all the runs' resulting statistics as
 * `double` arrays in separate (for each statistic) raw output files.
 * We can read each resulting output file into a NumPy (imported as `np`) float array by directly using `np.fromfile`.
 */
public class Main {
    // the number of simulations to run
    final static int SIMULATION_COUNT = 500_000;
    // the time at which to stop each simulation
    final static double STOP_TIME = 5000;
    // the rate at which customers arrive
    final static double ARRIVAL_RATE = 4;
    // the rate at which we serve customers
    final static double SERVICE_RATE = 5;
    // the number of statistics that each run returns
    final static int STATISTICS_COUNT = 3;
    // the file paths at which to store the resulting statistic arrays
    final static String[] FILE_PATHS = {
            "C:\\Users\\binma\\Documents\\output\\utilisations.dat",
            "C:\\Users\\binma\\Documents\\output\\mean_customers.dat",
            "C:\\Users\\binma\\Documents\\output\\mean_system_times.dat"
    };

    public static void main(String[] args) throws java.io.IOException {
        // initialise a byte buffer for each statistic
        ByteBuffer[] buffers = new ByteBuffer[STATISTICS_COUNT];
        for (int index = 0; index < STATISTICS_COUNT; index++) {
            buffers[index] = ByteBuffer.wrap(new byte[SIMULATION_COUNT * 8]);
            // The default endianness for a `ByteBuffer` is big but NumPy uses little endian floats.
            buffers[index].order(ByteOrder.LITTLE_ENDIAN);
        }

        // initialise a simulator under the given parameters
        CheckoutSimulator simulator = new CheckoutSimulator(STOP_TIME, ARRIVAL_RATE, SERVICE_RATE);
        for (int count = 0; count < SIMULATION_COUNT; count++) {
            if (count % 100_000 == 0) {
                System.out.println(count);
            }
            // run the simulator and collect the run's statistics
            double[] statistics = simulator.run();
            for (int index = 0; index < STATISTICS_COUNT; index++) {
                // write each statistic to its corresponding buffer
                buffers[index].putDouble(statistics[index]);
            }
        }

        // write each buffer to the file at its corresponding path
        for (int index = 0; index < STATISTICS_COUNT; index++) {
            // We rewind the buffer so that we write all its data.
            buffers[index].rewind();
            FileChannel channel = new FileOutputStream(FILE_PATHS[index]).getChannel();
            channel.write(buffers[index]);
            channel.close();
        }
    }
}
