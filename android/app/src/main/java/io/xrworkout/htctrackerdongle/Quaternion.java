package io.xrworkout.htctrackerdongle;

/**
 * Quaternions are data structures built from unicorn horns.
 *
 * I nabbed this implementation from The Internet.
 */
public final class Quaternion {
    private double x;
    private double y;
    private double z;
    private double w;
    //private float[] matrixs;

    public Quaternion(final Quaternion q) {
        this(q.x, q.y, q.z, q.w);
    }

    public Quaternion(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public void set(final Quaternion q) {
        //matrixs = null;
        this.x = q.x;
        this.y = q.y;
        this.z = q.z;
        this.w = q.w;
    }

    public Quaternion(Vector3 axis, double angle) {
        set(axis, angle);
    }

    public double norm() {
        return Math.sqrt(dot(this));
    }

    public double getW() {
        return w;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String toString() {

        return new String( String.format("%.3f | %.3f | %.3f | %.3f",x,y,z,w ) );

    }


    /**
     * @param axis
     *            rotation axis, unit vector
     * @param angle
     *            the rotation angle
     * @return this
     */
    public Quaternion set(Vector3 axis, double angle) {
        //matrixs = null;
        double s = (double) Math.sin(angle / 2);
        w = (double) Math.cos(angle / 2);
        x = axis.getX() * s;
        y = axis.getY() * s;
        z = axis.getZ() * s;
        return this;
    }

    public Quaternion mulThis(Quaternion q) {
        //matrixs = null;
        double nw = w * q.w - x * q.x - y * q.y - z * q.z;
        double nx = w * q.x + x * q.w + y * q.z - z * q.y;
        double ny = w * q.y + y * q.w + z * q.x - x * q.z;
        z = w * q.z + z * q.w + x * q.y - y * q.x;
        w = nw;
        x = nx;
        y = ny;
        return this;
    }

    public Quaternion scaleThis(double scale) {
        if (scale != 1) {
            //matrixs = null;
            w *= scale;
            x *= scale;
            y *= scale;
            z *= scale;
        }
        return this;
    }

    public Quaternion divThis(double scale) {
        if (scale != 1) {
            //matrixs = null;
            w /= scale;
            x /= scale;
            y /= scale;
            z /= scale;
        }
        return this;
    }

    public double dot(Quaternion q) {
        return x * q.x + y * q.y + z * q.z + w * q.w;
    }

    public boolean equals(Quaternion q) {
        return x == q.x && y == q.y && z == q.z && w == q.w;
    }

    public Quaternion interpolateThis(Quaternion q, double t) {
        if (!equals(q)) {
            double d = dot(q);
            double qx, qy, qz, qw;

            if (d < 0f) {
                qx = -q.x;
                qy = -q.y;
                qz = -q.z;
                qw = -q.w;
                d = -d;
            } else {
                qx = q.x;
                qy = q.y;
                qz = q.z;
                qw = q.w;
            }

            double f0, f1;

            if ((1 - d) > 0.1f) {
                double angle = (double) Math.acos(d);
                double s = (double) Math.sin(angle);
                double tAngle = t * angle;
                f0 = (double) Math.sin(angle - tAngle) / s;
                f1 = (double) Math.sin(tAngle) / s;
            } else {
                f0 = 1 - t;
                f1 = t;
            }

            x = f0 * x + f1 * qx;
            y = f0 * y + f1 * qy;
            z = f0 * z + f1 * qz;
            w = f0 * w + f1 * qw;
        }

        return this;
    }

    public Quaternion normalizeThis() {
        return divThis(norm());
    }

    public Quaternion interpolate(Quaternion q, double t) {
        return new Quaternion(this).interpolateThis(q, t);
    }

    /**
     * Converts this Quaternion into a matrix, returning it as a float array.
     */
    public float[] toMatrix() {
        float[] matrixs = new float[16];
        toMatrix(matrixs);
        return matrixs;
    }

    /**
     * Converts this Quaternion into a matrix, placing the values into the given array.
     * @param matrixs 16-length float array.
     */
    public final void toMatrix(float[] matrixs) {
        matrixs[3] = 0.0f;
        matrixs[7] = 0.0f;
        matrixs[11] = 0.0f;
        matrixs[12] = 0.0f;
        matrixs[13] = 0.0f;
        matrixs[14] = 0.0f;
        matrixs[15] = 1.0f;

        matrixs[0] = (float) (1.0f - (2.0f * ((y * y) + (z * z))));
        matrixs[1] = (float) (2.0f * ((x * y) - (z * w)));
        matrixs[2] = (float) (2.0f * ((x * z) + (y * w)));

        matrixs[4] = (float) (2.0f * ((x * y) + (z * w)));
        matrixs[5] = (float) (1.0f - (2.0f * ((x * x) + (z * z))));
        matrixs[6] = (float) (2.0f * ((y * z) - (x * w)));

        matrixs[8] = (float) (2.0f * ((x * z) - (y * w)));
        matrixs[9] = (float) (2.0f * ((y * z) + (x * w)));
        matrixs[10] = (float) (1.0f - (2.0f * ((x * x) + (y * y))));
    }



    //From Unity source because their euler angles work
    // Makes euler angles positive 0/360 with 0.0001 hacked to support old behaviour of QuaternionToEuler
    /*public Vector3 Internal_MakePositive(Vector3 euler)
    {
        float negativeFlip = -0.0001f;
        float positiveFlip = 2*Math.PI + negativeFlip;

        float x = euler.getX();
        float y = euler.getY();
        float z = euler.getZ();

        if (x < negativeFlip)
            x += 2*Math.PI;
        else if (x > positiveFlip)
            x -= 2*Math.PI;

        if (y < negativeFlip)
            y += 2*Math.PI;
        else if (y > positiveFlip)
            y -= 2*Math.PI;

        if (z < negativeFlip)
            z += 2*Math.PI;
        else if (z > positiveFlip)
            z -= 2*Math.PI;

        return new Vector3(x,y,z);
    }*/

    /** q1 does not need to be normalised*/
    public Vector3 toEuler() {
        /* old
        double sqw = w*w;
        double sqx = x*x;
        double sqy = y*y;
        double sqz = z*z;
        double yaw = Math.atan2(2.0 * (x*y + z*w),(sqx - sqy - sqz + sqw));
        double roll = Math.atan2(2.0 * (y*z + x*w),(-sqx - sqy + sqz + sqw));
        double pitch = Math.asin(-2.0 * (x*z - y*w)/(sqx + sqy + sqz + sqw));
        */

        /*
        // roll (x-axis rotation)
        double sinr_cosp = 2 * (w * x + y * z);
        double cosr_cosp = 1 - 2 * (x * x + y * y);
        double roll = Math.atan2(sinr_cosp, cosr_cosp);

        // pitch (y-axis rotation)
        double sinp = Math.sqrt(1 + 2 * (w * y - x * z));
        double cosp = Math.sqrt(1 - 2 * (w * y - x * z));
        double pitch = 2 * Math.atan2(sinp, cosp) - Math.PI / 2;

        // yaw (z-axis rotation)
        double siny_cosp = 2 * (w * z + x * y);
        double cosy_cosp = 1 - 2 * (y * y + z * z);
        double yaw = Math.atan2(siny_cosp, cosy_cosp);


        return new Vector3( (float)pitch, (float)yaw, (float)roll );
        */


        double ex;
        double ey;
        double ez;


        double qx = x;
        double qy = y;
        double qz = z;
        double qw = w;

        double _norm = Math.sqrt((qx * qx) + (qy * qy) + (qz * qz) + (qw * qw));

        if ( Math.abs(_norm) > 0.000001f )
        {
            qx = qx / _norm;
            qy = qy / _norm;
            qz = qz / _norm;
            qw = qw / _norm;
        }

        // if the input quaternion is normalized, this is exactly one. Otherwise, this acts as a correction factor for the quaternion's not-normalizedness
        double unit = (qx * qx) + (qy * qy) + (qz * qz) + (qw * qw);


        // this will have a magnitude of 0.5 or greater if and only if this is a singularity case
        double test = qx * qw - qy * qz;

        if (test > 0.4995f * unit) // singularity at north pole
        {
            ex = Math.PI / 2;
            ey = 2f * Math.atan2(qy, qx);
            ez = 0;
        }
        else if (test < -0.4995f * unit) // singularity at south pole
        {
            ex = -Math.PI / 2;
            ey = -2f * Math.atan2(qy, qx);
            ez = 0;
        }
        else // no singularity - this is the majority of cases
        {
            ex = Math.asin(2f * (qw * qx - qy * qz));
            ey = Math.atan2(2f * qw * qy + 2f * qz * qx, 1 - 2f * (qx * qx + qy * qy));
            ez = Math.atan2(2f * qw * qz + 2f * qx * qy, 1 - 2f * (qz * qz + qx * qx));
        }

        //...and then ensure the degree values are between 0 and 360
        ex = ex % ( 2*Math.PI );
        ey = ey % ( 2*Math.PI );
        ez = ez % ( 2*Math.PI );

        return new Vector3( (float)ex, (float)ey, (float)ez );


    }


    public Vector3 toEulerZXY() {
        /* convert to Euler angles assuming that the rotations are applied in the order of Z, X, Y */
        
        double qx = x;
        double qy = y;
        double qz = z;
        double qw = w;
        
        // Normalize the quaternion
        double norm = Math.sqrt(qx * qx + qy * qy + qz * qz + qw * qw);
        if (Math.abs(norm) > 0.000001) {
            qx = qx / norm;
            qy = qy / norm;
            qz = qz / norm;
            qw = qw / norm;
        }
        
        // For Z-X-Y rotation order, we need to extract angles in this specific sequence
        // The rotation matrix elements we need for Z-X-Y order are:
        // R21 = 2(qx*qy + qz*qw)
        // R22 = 1 - 2(qx*qx + qz*qz)  
        // R20 = 2(qy*qz - qx*qw)
        // R10 = 2(qx*qz + qy*qw)
        // R00 = 1 - 2(qy*qy + qz*qz)
        
        double r21 = 2.0 * (qx * qy + qz * qw);
        double r22 = 1.0 - 2.0 * (qx * qx + qz * qz);
        double r20 = 2.0 * (qy * qz - qx * qw);
        double r10 = 2.0 * (qx * qz + qy * qw);
        double r00 = 1.0 - 2.0 * (qy * qy + qz * qz);
        
        double ex, ey, ez;
        
        // Check for gimbal lock
        double test = Math.abs(r20);
        
        if (test > 0.99999) {
            // Gimbal lock case
            if (r20 > 0) {
                // Positive gimbal lock
                ex = Math.PI / 2.0;
                ey = 0.0;
                ez = Math.atan2(r21, r22);
            } else {
                // Negative gimbal lock
                ex = -Math.PI / 2.0;
                ey = 0.0;
                ez = Math.atan2(-r21, r22);
            }
        } else {
            // Normal case - no gimbal lock
            ex = Math.asin(r20);  // X rotation (pitch)
            ey = Math.atan2(-r10, r00);  // Y rotation (roll)  
            ez = Math.atan2(-r21, r22);  // Z rotation (yaw)
        }
        
        // Ensure angles are in the range [0, 2π)
        ex = ex % (2.0 * Math.PI);
        ey = ey % (2.0 * Math.PI);
        ez = ez % (2.0 * Math.PI);
        
        // Make angles positive
        if (ex < 0) ex += 2.0 * Math.PI;
        if (ey < 0) ey += 2.0 * Math.PI;
        if (ez < 0) ez += 2.0 * Math.PI;
        
        return new Vector3((float)ex, (float)ey, (float)ez);
    }



}
