package fr.charlotte.ksp.controller;

import fr.charlotte.ksp.utils.SevenSegment;
import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.services.SpaceCenter;
import org.apache.commons.math3.util.Precision;

import java.io.IOException;


public class TelemetryController extends Thread {

    private SevenSegment speedSevenSegment;

    public TelemetryController() throws IOException {
        this.speedSevenSegment = new SevenSegment((short) 1);
    }

    @Override
    public void run() {
        System.out.println("Starting Telemetry Monitoring Thread");
        Connection connection;
        try {
            connection = Connection.newInstance("localhost", "192.168.1.48", 50000, 50001);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        SpaceCenter sp = SpaceCenter.newInstance(connection);
        SpaceCenter.Vessel v;
        SpaceCenter.Control control;
        try {
            control = sp.getActiveVessel().getControl();
            v = sp.getActiveVessel();
        } catch (RPCException e) {
            System.out.println("You do not have any vessel active");
            return;
        }
        while (!interrupted()) {
            try {
                SpaceCenter.Flight f = v.flight(v.getOrbit().getBody().getReferenceFrame());
                double speed = Precision.round(f.getSpeed(), 3);
                speedSevenSegment.showNumber(speed);
                //   Thread.sleep(10);
            } catch (RPCException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
