package bello.andrea.accelerometertofile;

import org.json.JSONException;
import org.json.JSONObject;

public class Vector3D {

    private final static double ALPHA = 0.8;

    double x;
    double y;
    double z;

    public Vector3D() {
    }

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void applyLowPassFilter(double x, double y, double z){
        this.x = lowPass(this.x, x);
        this.y = lowPass(this.y, y);
        this.z = lowPass(this.z, z);
    }

    private static double lowPass(double calculated, double newValue){
        return ALPHA * calculated + (1 - ALPHA) * newValue;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void subtraction(Vector3D otherVector){
        this.x = this.x - otherVector.x;
        this.y = this.y - otherVector.y;
        this.z = this.z - otherVector.z;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("x", (int)this.x);
        result.put("y", (int)this.y);
        result.put("z", (int)this.z);
        return  result;
    }
}
