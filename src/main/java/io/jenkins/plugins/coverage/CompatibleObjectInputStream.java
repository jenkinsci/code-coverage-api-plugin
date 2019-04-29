package io.jenkins.plugins.coverage;

import java.io.*;

/**
 * {@link io.jenkins.plugins.coverage.targets.CoverageElement} didn't have serialVersionUID. In order to keep
 * backwards compatibility, use this class to read serialized Object to avoid that serialVersionUID is different.
 */
public class CompatibleObjectInputStream extends ObjectInputStream {

    public CompatibleObjectInputStream(InputStream in) throws IOException {
        super(in);
    }


    // See https://stackoverflow.com/questions/795470/how-to-deserialize-an-object-persisted-in-a-db-now-when-the-object-has-different?rq=1
    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass resultClassDescriptor = super.readClassDescriptor(); // initially streams descriptor
        Class localClass = Class.forName(resultClassDescriptor.getName()); // the class in the local JVM that this descriptor represents.
        ObjectStreamClass localClassDescriptor = ObjectStreamClass.lookup(localClass);
        if (localClassDescriptor != null) { // only if class implements serializable
            final long localSUID = localClassDescriptor.getSerialVersionUID();
            final long streamSUID = resultClassDescriptor.getSerialVersionUID();
            if (streamSUID != localSUID) { // check for serialVersionUID mismatch.
                String s = String.format("Overriding serialized class %s version mismatch: local serialVersionUID = %d stream serialVersionUID = %d",
                        resultClassDescriptor.getName(), localSUID, streamSUID);
                Exception e = new InvalidClassException(s);
                System.out.println("Potentially Fatal Deserialization Operation. " + e);
                resultClassDescriptor = localClassDescriptor; // Use local class descriptor for deserialization
            }
        }
        return resultClassDescriptor;
    }
}
