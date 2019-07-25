package plugins.fmp.toolsMulticafe;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.SwingConstants;
import javax.vecmath.Vector2d;

import edu.emory.mathcs.jtransforms.fft.FloatFFT_2D;
import flanagan.complex.Complex;

import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;


public class DufourRigidRegistration {

    /**
     * Enumeration of the various supported resizing policies for rigid registration
     * 
     * @author Alexandre Dufour
     */
    public enum ResizePolicy
    {
        /**
         * The final bounds of the registered sequence will be grown to ensure no information is
         * lost, while missing pixels will be given an arbitrary value (typically 0).
         */
        UNITE_BOUNDS,
        /**
         * The final bounds of the registered sequence will be identical to that of the initial
         * sequence, centered on the reference frame. Out-of-bounds pixels will be lost, while
         * missing pixels will be given an arbitrary value (typically 0).
         */
        PRESERVE_SIZE,
        /**
         * The final bounds of the registered sequence will be the intersection of all translated
         * bounds. Information will be lost, but the resulting sequence will have no missing pixel.
         */
        INTERSECT_BOUNDS;
        
        @Override
        public String toString()
        {
            return super.toString().toLowerCase().replace('_', ' ');
        }
    }
    
    /**
     * Register a sequence over time using a single frame as reference
     * 
     * @param sequence
     *            the sequence to register
     * @param referenceFrame
     *            the reference frame used to register other frames
     * @param referenceChannel
     *            the channel used to calculate the transform (or -1 to calculate an average
     *            transform using all channels)
     * @param referenceSlice
     *            the slice used to calculate the transform (or -1 to calculate an average transform
     *            using all slices)
     * @return <code>true</code> if the data has changed, <code>false</code> otherwise (i.e. the
     *         registration cannot be improved)
     */
    public static boolean correctTemporalTranslation2D(Sequence sequence, int referenceFrame, int referenceChannel, int referenceSlice)
    {
        Rectangle newBounds = correctTemporalTranslation2D(sequence, referenceFrame, referenceChannel, referenceSlice, ResizePolicy.PRESERVE_SIZE);
        return newBounds.equals(sequence.getBounds2D());
    }
    
    /**
     * Registers a 2D translation/drift in a sequence using a single frame as reference
     * 
     * @param sequence
     *            the sequence to register
     * @param referenceFrame
     *            the reference frame used to register other frames
     * @param referenceChannel
     *            the channel used to calculate the transform (or -1 to calculate an average
     *            transform using all channels)
     * @param referenceSlice
     *            the slice used to calculate the transform (or -1 to calculate an average transform
     *            using all slices)
     * @param policy
     *            indicates how to deal with out-of-bound and missing pixels (see
     *            {@link ResizePolicy}) for more details
     * @param status
     *            (<code>null</code> if not needed) a status object to let the user know where it's
     *            at (just like All Saints).
     * @return the smallest common bounds enclosing the registered data (or the original sequence
     *         bounds if no drift was detected)
     */
    public static Rectangle correctTemporalTranslation2D(Sequence sequence, int referenceFrame, int referenceChannel, int referenceSlice, ResizePolicy policy)
    {
//        if (status == null) status = new EzStatus();
        
        Rectangle initialBounds = sequence.getBounds2D();
        int sizeT = sequence.getSizeT();
        int sizeC = sequence.getSizeC();
        DataType dataType = sequence.getDataType_();
        
        Sequence output = policy == ResizePolicy.PRESERVE_SIZE ? sequence : new Sequence();
        
        // Step 1: compute and accumulate all translations
        
        ArrayList<Rectangle> translatedBounds = new ArrayList<Rectangle>(sizeT);
        
        for (int t = 0; t < sizeT; t++)  // status.setCompletion(++t / (double) sizeT))
        {
            if (Thread.currentThread().isInterrupted()) return initialBounds;
            
            if (t == referenceFrame)
            {
                translatedBounds.add(initialBounds);
            }
            else
            {
                Point translation = findTranslation2D(sequence, referenceChannel, referenceSlice, t, referenceFrame);
                
                if (translation.x != 0 || translation.y != 0)
                {
                    translatedBounds.add(new Rectangle(translation.x, translation.y, initialBounds.width, initialBounds.height));
                }
                else
                {
                    translatedBounds.add(initialBounds);
                }
            }
        }
        
        // Step 2: compute the final bounds
        
        Rectangle finalBounds = new Rectangle(initialBounds);
        
        if (policy == ResizePolicy.INTERSECT_BOUNDS) for (Rectangle bounds : translatedBounds)
        {
            finalBounds = finalBounds.intersection(bounds);
        }
        else if (policy == ResizePolicy.UNITE_BOUNDS) for (Rectangle bounds : translatedBounds)
        {
            finalBounds = finalBounds.union(bounds);
        }
        
        // Step 3: build the final sequence
        
        for (int t = 0; t < sizeT; t++) //status.setCompletion(++t / (double) sizeT))
        {
            if (Thread.currentThread().isInterrupted()) return initialBounds;
            
            Point newOrigin = translatedBounds.get(t).getLocation();
            newOrigin.translate(-finalBounds.x, -finalBounds.y);
            
            for (int z = 0; z < sequence.getSizeZ(t); z++)
            {
                IcyBufferedImage translatedImage = new IcyBufferedImage(finalBounds.width, finalBounds.height, sizeC, dataType);
                translatedImage.copyData(sequence.getImage(t, z), null, newOrigin);
                output.setImage(t, z, translatedImage);
            }
        }
        
        if (policy != ResizePolicy.PRESERVE_SIZE) sequence.copyDataFrom(output);
        
        return finalBounds;
    }
    
    /**
     * Registers a 2D translation/drift in a sequence using the previous frame as reference
     * 
     * @param sequence
     *            the sequence to register
     * @param referenceChannel
     *            the channel used to calculate the transform (or -1 to calculate an average
     *            transform using all channels)
     * @param referenceSlice
     *            the slice used to calculate the transform (or -1 to calculate an average transform
     *            using all slices)
     * @param policy
     *            indicates how to deal with out-of-bound and missing pixels (see
     *            {@link ResizePolicy}) for more details
     * @param status
     *            (<code>null</code> if not needed) a status object to let the user know where it's
     *            at (just like All Saints).
     * @return the smallest common bounds enclosing the registered data (or the original sequence
     *         bounds if no drift was detected)
     */
    public static Rectangle correctTemporalTranslation2D(Sequence sequence, int referenceChannel, int referenceSlice, ResizePolicy policy)
    {
//        if (status == null) status = new EzStatus();
        
        Rectangle initialBounds = sequence.getBounds2D();
        int sizeT = sequence.getSizeT();
        int sizeC = sequence.getSizeC();
        DataType dataType = sequence.getDataType_();
        
        Sequence output = policy == ResizePolicy.PRESERVE_SIZE ? sequence : new Sequence();
        
        // Step 1: compute and accumulate all translations
        
        ArrayList<Rectangle> translatedBounds = new ArrayList<Rectangle>(sizeT);
        
        // Don't process t=0, we know the answer...
        translatedBounds.add(initialBounds);
        
        // Start directly at t=1
        for (int t = 1; t < sizeT; t++) //status.setCompletion(++t / (double) sizeT))
        {
            if (Thread.currentThread().isInterrupted()) return initialBounds;
            
            Point translation = findTranslation2D(sequence, referenceChannel, referenceSlice, t, t - 1);
            
            if (translation.x != 0 || translation.y != 0)
            {
                translatedBounds.add(new Rectangle(translation.x, translation.y, initialBounds.width, initialBounds.height));
            }
            else
            {
                translatedBounds.add(initialBounds);
            }
        }
        
        // Step 2: compute the final bounds
        
        Rectangle cumulativeBounds = new Rectangle(initialBounds);
        Rectangle finalBounds = new Rectangle(initialBounds);
        
        if (policy == ResizePolicy.INTERSECT_BOUNDS) for (Rectangle bounds : translatedBounds)
        {
            cumulativeBounds.translate(bounds.x, bounds.y);
            finalBounds = finalBounds.intersection(cumulativeBounds);
        }
        else if (policy == ResizePolicy.UNITE_BOUNDS) for (Rectangle bounds : translatedBounds)
        {
            cumulativeBounds.translate(bounds.x, bounds.y);
            finalBounds = finalBounds.union(cumulativeBounds);
        }
        
        // Step 3: build the final sequence
        
        cumulativeBounds.setBounds(initialBounds);
        
        for (int t = 0; t < sizeT; t++) //status.setCompletion(++t / (double) sizeT))
        {
            if (Thread.currentThread().isInterrupted()) return initialBounds;
            
            Rectangle translated = translatedBounds.get(t);
            cumulativeBounds.translate(translated.x, translated.y);
            
            for (int z = 0; z < sequence.getSizeZ(t); z++)
            {
                IcyBufferedImage translatedImage = new IcyBufferedImage(finalBounds.width, finalBounds.height, sizeC, dataType);
                translatedImage.copyData(sequence.getImage(t, z), null, new Point(cumulativeBounds.x - finalBounds.x, cumulativeBounds.y - finalBounds.y));
                output.setImage(t, z, translatedImage);
            }
        }
        
        if (policy != ResizePolicy.PRESERVE_SIZE) sequence.copyDataFrom(output);
        
        return finalBounds;
    }
    
    /**
     * Finds the 2D translation between two frames of a given sequence and return the result as a
     * {@link Point} with integer coordinates (rounded). If the reference channel or slice is set to
     * <code>-1</code> (i.e. "ALL"), then an average translation is computed from all selected
     * channels and slices
     * 
     * @param sequence
     * @param referenceChannel
     * @param referenceSlice
     * @param candidateFrame
     * @param referenceFrame
     * @return
     */
    private static Point findTranslation2D(Sequence sequence, int referenceChannel, int referenceSlice, int candidateFrame, int referenceFrame)
    {
        Vector2d translation = new Vector2d();
        
        int n = 0;
        int minZ = referenceSlice == -1 ? 0 : referenceSlice;
        int maxZ = referenceSlice == -1 ? sequence.getSizeZ(candidateFrame) : referenceSlice;
        for (int z = minZ; z <= maxZ; z++)
        {
            int minC = referenceChannel == -1 ? 0 : referenceChannel;
            int maxC = referenceChannel == -1 ? sequence.getSizeC() - 1 : referenceChannel;
            
            for (int c = minC; c <= maxC; c++)
            {
                IcyBufferedImage img = sequence.getImage(candidateFrame, z);
                IcyBufferedImage ref = sequence.getImage(referenceFrame, z);
                
                translation.add(findTranslation2D(img, c, ref, c));
                n++;
            }
        }
        if (n > 1) translation.scale(1.0 / n);
        
        return new Point((int) Math.round(translation.x), (int) Math.round(translation.y));
    }

    /**
     * @param source
     *            the source image
     * @param sourceC
     *            the channel in the source image
     * @param target
     *            the target image
     * @param targetC
     *            the channel in the target image
     * @return the translation vector needed to transform source into target
     */
    public static Vector2d findTranslation2D(IcyBufferedImage source, int sourceC, IcyBufferedImage target, int targetC)
    {
        if (!source.getBounds().equals(target.getBounds())) throw new UnsupportedOperationException("Cannot register images of different size (yet)");
        
        int width = source.getWidth();
        int height = source.getHeight();
        
        float[] _source = Array1DUtil.arrayToFloatArray(source.getDataXY(sourceC), source.isSignedDataType());
        float[] _target = Array1DUtil.arrayToFloatArray(target.getDataXY(targetC), target.isSignedDataType());
        
        float[] correlationMap = spectralCorrelation(_source, _target, width, height);
        
        // IcyBufferedImage corr = new IcyBufferedImage(width, height, new
        // float[][]{correlationMap});
        // Icy.getMainInterface().addSequence(new Sequence(corr));
        
        // Find maximum correlation
        
        int argMax = argMax(correlationMap, correlationMap.length);
        
        int transX = argMax % width;
        int transY = argMax / width;
        
        if (transX > width / 2) transX -= width;
        if (transY > height / 2) transY -= height;
        
        // recover (x,y)
        return new Vector2d(-transX, -transY);
    }
    private static float[] spectralCorrelation(float[] a1, float[] a2, int width, int height)
    {
        // JTransforms's FFT takes dimensions as (rows, columns)
        FloatFFT_2D fft = new FloatFFT_2D(height, width);
        
        return spectralCorrelation(a1, a2, width, height, fft);
    }
    
    private static float[] spectralCorrelation(float[] a1, float[] a2, int width, int height, FloatFFT_2D fft)
    {
        // FFT on images
        float[] sourceFFT = forwardFFT(a1, fft);
        float[] targetFFT = forwardFFT(a2, fft);
        
        // Compute correlation
        
        Complex c1 = new Complex(), c2 = new Complex();
        for (int i = 0; i < sourceFFT.length; i += 2)
        {
            c1.setReal(sourceFFT[i]);
            c1.setImag(sourceFFT[i + 1]);
            
            c2.setReal(targetFFT[i]);
            c2.setImag(targetFFT[i + 1]);
            
            // correlate c1 and c2 (no need to normalize)
            c1.timesEquals(c2.conjugate());
            
            sourceFFT[i] = (float) c1.getReal();
            sourceFFT[i + 1] = (float) c1.getImag();
        }
        
        // IFFT
        
        return inverseFFT(sourceFFT, fft);
    }
    /**
     * @param array
     * @param n
     *            limits the argMax to the first n elements of the input array
     * @return
     */
    private static int argMax(float[] array, int n)
    {
        int argMax = 0;
        float max = array[0];
        for (int i = 1; i < n; i++)
        {
            float val = array[i];
            if (val > max)
            {
                max = val;
                argMax = i;
            }
        }
        return argMax;
    }
 
    /**
     * Apply a (forward) FFT on real data.
     * 
     * @param data
     *            the data to transform.
     * @param fft
     *            An FFT object to perform the transform
     * @return the complex, Fourier-transformed data.
     */
    private static float[] forwardFFT(float[] realData, FloatFFT_2D fft)
    {
        float[] out = new float[realData.length * 2];
        
        // format the input as a complex array
        // => real and imaginary values are interleaved
        for (int i = 0, j = 0; i < realData.length; i++, j += 2)
            out[j] = realData[i];
        
        fft.complexForward(out);
        return out;
    }
    
    /**
     * Apply an inverse FFT on complex data.
     * 
     * @param data
     *            the complex data to transform.
     * @param fft
     *            An FFT object to perform the transform
     * @return the real, Fourier-inverse data.
     */
    private static float[] inverseFFT(float[] cplxData, FloatFFT_2D fft)
    {
        float[] out = new float[cplxData.length / 2];
        
        fft.complexInverse(cplxData, true);
        
        // format the input as a real array
        // => skip imaginary values
        for (int i = 0, j = 0; i < cplxData.length; i += 2, j++)
            out[j] = cplxData[i];
        
        return out;
    }
    
    /**
     * Register a sequence over time using the previous frame as reference
     * 
     * @param sequence
     *            the sequence to register
     * @param referenceChannel
     *            the channel used to calculate the transform (or -1 to calculate an average
     *            transform using all channels)
     * @param referenceSlice
     *            the slice used to calculate the transform (or -1 to calculate an average transform
     *            using all slices)
     * @return <code>true</code> if the data has changed, <code>false</code> otherwise (i.e. the
     *         registration cannot be improved)
     */
    public static boolean correctTemporalTranslation2D(Sequence sequence, int referenceChannel, int referenceSlice)
    {
        boolean change = false;
        
        for (int t = 1; t < sequence.getSizeT(); t++)
        {
            int referenceFrame = t - 1;
            
            if (Thread.currentThread().isInterrupted()) return change;
            
            Vector2d translation = new Vector2d();
            int n = 0;
            
            int minZ = referenceSlice == -1 ? 0 : referenceSlice;
            int maxZ = referenceSlice == -1 ? sequence.getSizeZ(t) : referenceSlice;
            
            for (int z = minZ; z <= maxZ; z++)
            {
                int minC = referenceChannel == -1 ? 0 : referenceChannel;
                int maxC = referenceChannel == -1 ? sequence.getSizeC() - 1 : referenceChannel;
                
                for (int c = minC; c <= maxC; c++)
                {
                    IcyBufferedImage img = sequence.getImage(t, z);
                    IcyBufferedImage ref = sequence.getImage(referenceFrame, z);
                    
                    translation.add(findTranslation2D(img, c, ref, c));
                    n++;
                }
            }
            
            translation.scale(1.0 / n);
//            System.out.println("[Rigid Registration] Translation: " + StringUtil.toString(translation.x, 2) + " / " + StringUtil.toString(translation.y, 2));
            
            if (translation.lengthSquared() != 0)
            {
                change = true;
                
                for (int z = 0; z < sequence.getSizeZ(t); z++)
                    sequence.setImage(t, z, applyTranslation2D(sequence.getImage(t, z), -1, translation, true));
            }
        }
        
        return change;
    }
    
    /**
     * Translates the specified sequence
     * 
     * @param seq
     *            the sequence to translate
     * @param t
     *            the frame to translate (or -1 for all)
     * @param z
     *            the frame to translate (or -1 for all)
     * @param c
     *            the frame to translate (or -1 for all)
     * @param preserveImageSize
     */
    public static void applyTranslation2D(Sequence seq, int t, int z, int c, Vector2d vector, boolean preserveImageSize)
    {
        if (vector.lengthSquared() == 0.0) return;
        
        int minT = (t == -1 ? 0 : t), maxT = (t == -1 ? seq.getSizeT() - 1 : t);
        int minZ = (z == -1 ? 0 : z), maxZ = (z == -1 ? seq.getSizeZ() - 1 : z);
        
        for (int time = minT; time <= maxT; time++)
            for (int slice = minZ; slice <= maxZ; slice++)
            {
                IcyBufferedImage image = seq.getImage(time, slice);
                image = applyTranslation2D(image, c, vector, preserveImageSize);
                seq.setImage(time, slice, image);
            }
    }
 
    public static IcyBufferedImage applyTranslation2D(IcyBufferedImage image, int channel, Vector2d vector, boolean preserveImageSize)
    {
        int dx = (int) Math.round(vector.x);
        int dy = (int) Math.round(vector.y);
        
        if (dx == 0 && dy == 0) return image;
        
        Rectangle newSize = image.getBounds();
        newSize.width += Math.abs(dx);
        newSize.height += Math.abs(dy);
        
        Point dstPoint_shiftedChannel = new Point(Math.max(0, dx), Math.max(0, dy));
        Point dstPoint_otherChannels = new Point(Math.max(0, -dx), Math.max(0, -dy));
        
        IcyBufferedImage newImage = new IcyBufferedImage(newSize.width, newSize.height, image.getSizeC(), image.getDataType_());
        for (int c = 0; c < image.getSizeC(); c++)
        {
            Point dstPoint = (channel == -1 || c == channel) ? dstPoint_shiftedChannel : dstPoint_otherChannels;
            newImage.copyData(image, null, dstPoint, c, c);
        }
        
        if (preserveImageSize)
        {
            newSize = image.getBounds();
            newSize.x = Math.max(0, -dx);
            newSize.y = Math.max(0, -dy);
            
            return IcyBufferedImageUtil.getSubImage(newImage, newSize);
        }
        return newImage;
    }
    
    /**
     * Register a sequence over time using the previous frame as reference
     * 
     * @param sequence
     *            the sequence to register
     * @param referenceChannel
     *            the channel used to calculate the transform (or -1 to calculate an average
     *            transform using all channels)
     * @param referenceSlice
     *            the slice used to calculate the transform (or -1 to calculate an average transform
     *            using all slices)
     * @return <code>true</code> if the data has changed, <code>false</code> otherwise (i.e. the
     *         registration cannot be improved)
     */
    public static boolean correctTemporalRotation2D(Sequence sequence, int referenceChannel, int referenceSlice)
    {
        boolean change = false;
        
        for (int t = 1; t < sequence.getSizeT(); t++)
        {
            int referenceFrame = t - 1;
            
            if (Thread.currentThread().isInterrupted()) return change;
            
            double angle = 0.0;
            int n = 0;
            
            int minZ = referenceSlice == -1 ? 0 : referenceSlice;
            int maxZ = referenceSlice == -1 ? sequence.getSizeZ(t) : referenceSlice;
            
            for (int z = minZ; z <= maxZ; z++)
            {
                int minC = referenceChannel == -1 ? 0 : referenceChannel;
                int maxC = referenceChannel == -1 ? sequence.getSizeC() : referenceChannel;
                
                for (int c = minC; c <= maxC; c++)
                {
                    IcyBufferedImage img = sequence.getImage(t, z);
                    IcyBufferedImage ref = sequence.getImage(referenceFrame, z);
                    
                    angle += findRotation2D(img, c, ref, c);
                    n++;
                }
            }
            
            angle /= n;
 //           System.out.println("[Rigid Registration] Angle: " + angle);
            
            if (angle != 0.0)
            {
                change = true;
                
                for (int z = 0; z < sequence.getSizeZ(t); z++)
                    sequence.setImage(t, z, applyRotation2D(sequence.getImage(t, z), -1, angle, true));
            }
        }
        
        return change;
    }
    
    /**
     * @param source
     *            the source image
     * @param sourceC
     *            the channel in the source image
     * @param target
     *            the target image
     * @param targetC
     *            the channel in the target image
     * @return the rotation angle around the center (in radians) that transforms source into target
     */
    public static double findRotation2D(IcyBufferedImage source, int sourceC, IcyBufferedImage target, int targetC)
    {
        return findRotation2D(source, sourceC, target, targetC, null);
    }
    
    /**
     * @param source
     *            the source image
     * @param sourceC
     *            the channel in the source image
     * @param target
     *            the target image
     * @param targetC
     *            the channel in the target image
     * @param previousTranslation
     *            the previous translation (if any) necessary to register the source to the target
     * @return the rotation angle around the center (in radians) that transforms source into target
     */
    public static double findRotation2D(IcyBufferedImage source, int sourceC, IcyBufferedImage target, int targetC, Vector2d previousTranslation)
    {
        if (!source.getBounds().equals(target.getBounds()))
        {
            // Both sizes are different. What to do?
            
            if (previousTranslation != null)
            {
                // the source has most probably been translated previously, let's grow the target
                // accordingly
                // (just need to know where the original data has to go)
                int xAlign = previousTranslation.x > 0 ? SwingConstants.LEFT : SwingConstants.RIGHT;
                int yAlign = previousTranslation.y > 0 ? SwingConstants.TOP : SwingConstants.BOTTOM;
                target = IcyBufferedImageUtil.scale(target, source.getSizeX(), source.getSizeY(), false, xAlign, yAlign);
            }
            
            else throw new UnsupportedOperationException("Cannot register images of different size (yet)");
        }
        
        // Convert to Log-Polar
        
        IcyBufferedImage sourceLogPol = toLogPolar(source.getImage(sourceC));
        IcyBufferedImage targetLogPol = toLogPolar(target.getImage(targetC));
        
        int width = sourceLogPol.getWidth(), height = sourceLogPol.getHeight();
        
        float[] _sourceLogPol = sourceLogPol.getDataXYAsFloat(0);
        float[] _targetLogPol = targetLogPol.getDataXYAsFloat(0);
        
        // Compute spectral correlation
        
        float[] correlationMap = spectralCorrelation(_sourceLogPol, _targetLogPol, width, height);
        
        // Find maximum correlation (=> rotation)
        
        int argMax = argMax(correlationMap, correlationMap.length / 2);
        
        // rotation is given along the X axis
        int rotX = argMax % width;
        
        if (rotX > width / 2) rotX -= width;
        
        return -rotX * 2 * Math.PI / width;
    }
    
    /**
     * Creates a Log-Polar view of the input image with default center (= image center) and
     * precision (360)
     * 
     * @return A Log-Polar view of the input image
     */
    private static IcyBufferedImage toLogPolar(IcyBufferedImage image)
    {
        return toLogPolar(image, image.getWidth() / 2, image.getHeight() / 2, 1080, 360);
    }
    
    /**
     * @param image
     * @param sizeTheta
     *            number of sectors
     * @param sizeRho
     *            number of rings
     * @return
     */
    private static IcyBufferedImage toLogPolar(IcyBufferedImage image, int centerX, int centerY, int sizeTheta, int sizeRho)
    {
        int sizeC = image.getSizeC();
        
        // create the log-polar image (X = theta, Y = rho)
        
        // theta: number of sectors
        double theta = 0.0, dtheta = 2 * Math.PI / sizeTheta;
        // pre-compute all sine/cosines
        float[] cosTheta = new float[sizeTheta];
        float[] sinTheta = new float[sizeTheta];
        for (int thetaIndex = 0; thetaIndex < sizeTheta; thetaIndex++, theta += dtheta)
        {
            cosTheta[thetaIndex] = (float) Math.cos(theta);
            sinTheta[thetaIndex] = (float) Math.sin(theta);
        }
        
        // rho: number of rings
        float drho = (float) (Math.sqrt(centerX * centerX + centerY * centerY) / sizeRho);
        
        IcyBufferedImage logPol = new IcyBufferedImage(sizeTheta, sizeRho, sizeC, DataType.FLOAT);
        
        for (int c = 0; c < sizeC; c++)
        {
            float[] out = logPol.getDataXYAsFloat(c);
            
            // first ring (rho=0): center value
            Array1DUtil.fill(out, 0, sizeTheta, getPixelValue(image, centerX, centerY, c));
            
            // Other rings
            float rho = drho;
            int outOffset = sizeTheta;
            for (int rhoIndex = 1; rhoIndex < sizeRho; rhoIndex++, rho += drho)
                for (int thetaIndex = 0; thetaIndex < sizeTheta; thetaIndex++, outOffset++)
                {
                    double x = centerX + rho * cosTheta[thetaIndex];
                    double y = centerY + rho * sinTheta[thetaIndex];
                    out[outOffset] = getPixelValue(image, x, y, c);
                }
        }
        
        logPol.updateChannelsBounds();
        return logPol;
    }

    /**
     * Calculates the 2D image value at the given real coordinates by bilinear interpolation
     * 
     * @param imageFloat
     *            the image to sample (must be of type {@link DataType#DOUBLE})
     * @param x
     *            the X-coordinate of the point
     * @param y
     *            the Y-coordinate of the point
     * @return the interpolated image value at the given coordinates
     */
    private static float getPixelValue(IcyBufferedImage img, double x, double y, int c)
    {
        int width = img.getWidth();
        int height = img.getHeight();
        Object data = img.getDataXY(c);
        DataType type = img.getDataType_();
        
        // "center" the coordinates to the center of the pixel
        x -= 0.5;
        y -= 0.5;
        
        int i = (int) Math.floor(x);
        int j = (int) Math.floor(y);
        
        if (i <= 0 || i >= width - 1 || j <= 0 || j >= height - 1) return 0f;
        
        float value = 0;
        
        final int offset = i + j * width;
        final int offset_plus_1 = offset + 1; // saves 1 addition
        
        x -= i;
        y -= j;
        
        final double mx = 1 - x;
        final double my = 1 - y;
        
        value += mx * my * Array1DUtil.getValueAsFloat(data, offset, type);
        value += x * my * Array1DUtil.getValueAsFloat(data, offset_plus_1, type);
        value += mx * y * Array1DUtil.getValueAsFloat(data, offset + width, type);
        value += x * y * Array1DUtil.getValueAsFloat(data, offset_plus_1 + width, type);
        
        return value;
    }
    
    /**
     * Rotates the specified sequence
     * 
     * @param seq
     *            the sequence to translate
     * @param t
     *            the frame to translate (or -1 for all)
     * @param z
     *            the frame to translate (or -1 for all)
     * @param c
     *            the frame to translate (or -1 for all)
     * @param preserveImageSize
     */
    public static void applyRotation2D(Sequence seq, int t, int z, int c, double angle, boolean preserveImageSize)
    {
        if (angle == 0.0) return;
        
        int minT = (t == -1 ? 0 : t), maxT = (t == -1 ? seq.getSizeT() - 1 : t);
        int minZ = (z == -1 ? 0 : z), maxZ = (z == -1 ? seq.getSizeZ() - 1 : z);
        
        for (int time = minT; time <= maxT; time++)
            for (int slice = minZ; slice <= maxZ; slice++)
            {
                IcyBufferedImage image = seq.getImage(time, slice);
                image = applyRotation2D(image, c, angle, preserveImageSize);
                seq.setImage(time, slice, image);
            }
    }
    
    /**
     * @param img
     * @param channel
     *            the channel to rotate (or -1 for all)
     * @param angle
     * @return
     */
    public static IcyBufferedImage applyRotation2D(IcyBufferedImage img, int channel, double angle, boolean preserveImageSize)
    {
        if (angle == 0.0) return img;
        
        // start with the rotation to calculate the largest bounds
        IcyBufferedImage rotImg = IcyBufferedImageUtil.rotate(img.getImage(channel), angle);
        
        // calculate the difference in bounds
        Rectangle oldSize = img.getBounds();
        Rectangle newSize = rotImg.getBounds();
        int dw = (newSize.width - oldSize.width) / 2;
        int dh = (newSize.height - oldSize.height) / 2;
        
        if (channel == -1 || img.getSizeC() == 1)
        {
            if (preserveImageSize)
            {
                oldSize.translate(dw, dh);
                return IcyBufferedImageUtil.getSubImage(rotImg, oldSize);
            }
            return rotImg;
        }
        
        IcyBufferedImage[] newImages = new IcyBufferedImage[img.getSizeC()];
        
        if (preserveImageSize)
        {
            for (int c = 0; c < newImages.length; c++)
                if (c == channel)
                {
                    // crop the rotated channel
                    oldSize.translate(dw, dh);
                    newImages[c] = IcyBufferedImageUtil.getSubImage(rotImg, oldSize);
                }
                else newImages[c] = img.getImage(c);
        }
        else
        {
            for (int c = 0; c < newImages.length; c++)
                if (c != channel)
                {
                    // enlarge and center the non-rotated channels
                    newImages[c] = new IcyBufferedImage(newSize.width, newSize.height, 1, img.getDataType_());
                    newImages[c].copyData(img.getImage(c), null, new Point(dw, dh));
                }
                else newImages[channel] = rotImg;
        }
        
        return IcyBufferedImage.createFrom(Arrays.asList(newImages));
    }

}
