package fr.charlotte.ksp;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.platform.PlatformAlreadyAssignedException;
import fr.charlotte.ksp.controller.TelemetryController;
import fr.charlotte.ksp.controller.ThrustController;
import fr.colin.seesawsdk.Seesaw;
import fr.colin.seesawsdk.modules.AnalogModule;
import fr.colin.seesawsdk.utils.Pins;
import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.services.SpaceCenter;
import org.apache.commons.math3.util.Precision;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.interrupted;
import static java.lang.Thread.sleep;

public class KSPController {

    public static final Map<String, Thread> threads = new ConcurrentHashMap<>();

    private static Seesaw seesaw;
    private static AnalogModule analogModule;

    public static void main(String... args) throws IOException, RPCException {
        try (Connection connection = Connection.newInstance("localhost", "192.168.1.48", 50000, 50001)) {
            SpaceCenter spaceCenter = SpaceCenter.newInstance(connection);
            SpaceCenter.Vessel v;
            while (true) {
                try {
                    if (spaceCenter.getActiveVessel() != null) {
                        v = spaceCenter.getActiveVessel();
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
            System.out.println("Connected to the vessel " + v.getName());

            seesaw = new Seesaw(I2CBus.BUS_1);
            seesaw.init();
            analogModule = new AnalogModule(seesaw);

            startThrust();
            startTelemetryMonitoring();

        } catch (I2CFactory.UnsupportedBusNumberException | PlatformAlreadyAssignedException e) {
            e.printStackTrace();
        }
    }

    public static void startTelemetryMonitoring() throws IOException {
        if (threads.containsKey("telemetry")) {
            threads.get("telemetry").interrupt();
            threads.remove("telemetry");
        }
        TelemetryController telemetryController = new TelemetryController();
        threads.put("telemetry", telemetryController);
        telemetryController.start();
    }

    public static void startThrust() {
        if (threads.containsKey("thrust")) {
            threads.get("thrust").interrupt();
            threads.remove("thrust");
        }
        ThrustController thurst = new ThrustController(analogModule);
        threads.put("thrust", thurst);
        thurst.start();
    }

}
