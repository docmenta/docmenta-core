/*
 * TestApp.java
 * 
 *  Copyright (C) 2013  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.docma.coreapi.implementation;

import org.docma.coreapi.*;
import java.io.*;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import javax.imageio.*;
import javax.media.jai.*;

/**
 *
 * @author MP
 */
public class TestApp {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception
    {
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");  // disable native mode
        
        DocImageRendition rendition = new DocImageRendition("thumb",
                                                            DocImageRendition.FORMAT_PNG,
                                                            128, 128);

        File image_dir = new File("C:\\TEMP\\image_io_test");
        File fin = new File(image_dir, "Sonnenuntergang.jpg");
        File fout = new File(image_dir, "imageout.png");
        createRendition(fout, fin, rendition);
        if (! image_dir.renameTo(new File("C:\\TEMP\\image_test_renamed"))) {
            System.out.println("Could not rename dir!");
        }
    }

    public static void createRendition(File fout, File image, DocImageRendition rendition) throws Exception
    {
        BufferedImage sourceImage = ImageIO.read(image);
        BufferedImage renditionImage = render(sourceImage, rendition);
        RenderedOp ro = JAI.create("filestore", renditionImage, fout.getAbsolutePath(), rendition.getFormat(), null);
        ro.dispose();
    }

    private static BufferedImage render(RenderedImage image, DocImageRendition rendition)
    {
        float scale = 1.0F;
        int img_width = image.getWidth();
        int img_height = image.getHeight();

        if ((img_width > rendition.getMaxWidth()) || (img_height > rendition.getMaxHeight())) {
            float s1 = (float)rendition.getMaxWidth() / (float)img_width;
            float s2 = (float)rendition.getMaxHeight() / (float)img_height;

            if ( s1 < s2 ) {
                scale = s1;
            } else {
                scale = s2;
            }
        }

        InterpolationBilinear interp = new InterpolationBilinear();

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(scale);
        pb.add(scale);
        pb.add(0.0F);
        pb.add(0.0F);
        pb.add(interp);
        // This is needed to remove the block border around the images:
        // RenderingHints hints =
        //        new RenderingHints(JAI.KEY_BORDER_EXTENDER,
        //        BorderExtender.createInstance(BorderExtender.BORDER_REFLECT));

        PlanarImage temp = JAI.create("scale", pb);
        // deprecated call:
        // PlanarImage temp = JAI.create("scale",
        //                                image,
        //                                scale,
        //                                scale,
        //                                0.0F,
        //                                0.0F,
        //                                interp);

        return temp.getAsBufferedImage();
    }

}
