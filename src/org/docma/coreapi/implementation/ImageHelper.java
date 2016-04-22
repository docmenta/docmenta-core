/*
 * ImageHelper.java
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

import java.io.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.imageio.ImageIO;
import javax.media.jai.*;

import org.docma.coreapi.*;

/**
 *
 * @author MP
 */
public class ImageHelper
{

    public static void createRendition(File fout, File image, DocImageRendition rendition) throws Exception
    {
        BufferedImage sourceImage = ImageIO.read(image);
        BufferedImage renditionImage = render(sourceImage, rendition);
        RenderedOp ro = JAI.create("filestore", renditionImage, fout.getAbsolutePath(), rendition.getFormat(), null);
        if (ro != null) ro.dispose();  // this is required to release file handle to fout
    }

    public static void createRendition(OutputStream out, InputStream image, DocImageRendition rendition) throws Exception
    {
        BufferedImage sourceImage = ImageIO.read(image);
        BufferedImage renditionImage = render(sourceImage, rendition);
        JAI.create("encode", renditionImage, out, rendition.getFormat(), null);
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
