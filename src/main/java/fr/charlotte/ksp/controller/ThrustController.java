package fr.charlotte.ksp.controller;

import fr.colin.seesawsdk.Seesaw;
import fr.colin.seesawsdk.modules.AnalogModule;
import fr.colin.seesawsdk.utils.Pins;
import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.services.SpaceCenter;

import java.io.IOException;

public class ThrustController extends Thread {

    private AnalogModule analogController;

    public ThrustController(AnalogModule analogController){
        this.analogController = analogController;
    }

    @Override
    public void run() {
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
                int i = analogController.readChannel(Pins.ADC0); //Return 0 to 1024
                float sd = (float) i / 1024f;
                control.setThrottle(sd);
                sleep(10);
            } catch (RPCException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
