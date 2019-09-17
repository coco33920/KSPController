package fr.colin.ksp;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.platform.PlatformAlreadyAssignedException;
import fr.colin.seesawsdk.Seesaw;
import fr.colin.seesawsdk.modules.AnalogModule;
import fr.colin.seesawsdk.utils.Pins;
import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.services.SpaceCenter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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
                if (spaceCenter.getActiveVessel() != null) {
                    v = spaceCenter.getActiveVessel();
                    break;
                }
            }
            System.out.println("Connected to the vessel " + v.getName());
            SpaceCenter.Control control = v.getControl();
            seesaw = new Seesaw(I2CBus.BUS_1);
            seesaw.init();
            analogModule = new AnalogModule(seesaw);
            startThrust();

        } catch (I2CFactory.UnsupportedBusNumberException | PlatformAlreadyAssignedException e) {
            e.printStackTrace();
        }
    }

    public static void startThrust() {
        if (threads.containsKey("thrust")) {
            threads.get("thrust").interrupt();
            threads.remove("thrust");
        }
        Thread t = new Thread(() -> {
            System.out.println("Start Thrust control thread");
            Connection connection;
            try {
                connection = Connection.newInstance("localhost", "192.168.1.48", 50000, 50001);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            SpaceCenter sp = SpaceCenter.newInstance(connection);
            SpaceCenter.Control control;
            try {
                control = sp.getActiveVessel().getControl();
            } catch (RPCException e) {
                System.out.println("You do not have any vessel active");
                return;
            }
            while (!interrupted()) {
                try {
                    int i = analogModule.readChannel(Pins.ADC0); //Return 0 to 1024
                    float sd = (float) i / 1024f;
                    control.setThrottle(sd);
                    sleep(10);
                } catch (RPCException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        threads.put("thrust", t);
        t.start();
    }

}
