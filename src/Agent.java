import processing.core.PVector;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created at 14/07/16
 *
 * @author tmshv
 */
public class Agent implements IInterest {
    PVector location = new PVector();
    protected PVector velocity = new PVector();
    protected PVector acceleration = new PVector();

    public float maxForce;    // Maximum steering force
    public float maxSpeed;    // Maximum speed
    public float interestDistance = 50;
    public float interestThreshold = 0.001f;
    public float interestMultiplier = 0.9f;

    HashMap<IInterest, Float> interestValues;

    Track track;
    int color;
    int mass = 5;

    boolean moving = true;
    ArrayList<String> interactTypes;
    String type;

    public Agent(String type, float maxForce, float maxSpeed, int color) {
        this.type = type;
        this.maxForce = maxForce;
        this.maxSpeed = maxSpeed;
        this.color = color;
        this.track = new Track(color);
        interestValues = new HashMap<>();
        interactTypes = new ArrayList<>();
    }

    public float getInteractionPower(Agent other) {
        return 10;
    }

    public void interact(Agent other) {
        if (!interactTypes.contains(other.getType())) return;

        float power = getInteractionPower(other);
        if (power != 0) {
            PVector v = getSteeringDirection(other.location);
            v.normalize();
            v.mult(power);
            applyForce(v);
        }
    }

    public void interact(Attractor attractor) {
        if (!interactTypes.contains(attractor.getType())) return;

        float dist = PVector.dist(location, attractor.getLocation());
        if (dist < interestDistance) {
            float force = getInterestValueFor(attractor);

            PVector dir = getSteeringDirection(attractor.getLocation());
            dir.normalize();
            dir.mult(force);
            applyForce(dir);
        }
    }

    public void run() {
        update();
        track.write(this.location.copy());
    }

    void seek(PVector target) {
        PVector steer = getSteeringDirection(target);
        applyForce(steer);
    }

    /**
     * A method that calculates and applies a steering force towards a target
     * STEER = DESIRED MINUS VELOCITY
     *
     * @param target
     * @return
     */
    PVector getSteeringDirection(PVector target) {
        PVector desired = PVector.sub(target, location);

        // If the magnitude of desired equals 0, skip out of here
        // (We could optimize this to check if x and y are 0 to avoid mag() square root
        if (desired.mag() == 0) return new PVector();

        // Normalize desired and scale to maximum speed
        desired.normalize();
        desired.mult(maxSpeed);

        // Steering = Desired minus Velocity
        PVector steer = PVector.sub(desired, velocity);
        steer.limit(maxForce);  // Limit to maximum steering force

        return steer;
    }


    /**
     * Method to update location
     */
    public void update() {
        // println("velocity: "+velocity);

        // Update velocity
        velocity.add(acceleration);
        // Limit speed
        velocity.limit(maxSpeed);
        location.add(velocity);
        // Reset accelertion to 0 each cycle
        acceleration.mult(0);
    }

    public void applyForce(PVector force) {
        // We could add mass here if we want A = F / M
        acceleration.add(force);
    }

    public String getType() {
        return type;
    }

    @Override
    public float getValue() {
        return mass;
    }

    @Override
    public float getInterestValueFor(IInterest other) {
        float v = other.getValue();
        if (interestValues.containsKey(other)) {
            v = interestValues.get(other);
            v *= interestMultiplier;
            if(v < interestThreshold) v = 0;
        }
        interestValues.put(other, v);
        return v;
    }

    public void addInteractionType(String type) {
        interactTypes.add(type);
    }
}