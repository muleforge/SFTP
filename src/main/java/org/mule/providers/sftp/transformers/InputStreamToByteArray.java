/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.sftp.transformers;

import org.apache.commons.io.IOUtils;
import org.mule.config.i18n.Message;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * TODO
 */
public class InputStreamToByteArray extends AbstractTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -7444711427779720031L;
    
    public InputStreamToByteArray()
    {
        registerSourceType(InputStream.class);
        setReturnClass(byte[].class);
    }

    public Object doTransform(Object msg, String encoding) throws TransformerException
    {
        if (msg instanceof InputStream)
        {
        	InputStream inputStream = null;


            try
            {
            	inputStream = (InputStream) msg;
            	
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                	
                    byte [] buffer = new byte[1024];
                    int len;
                    while( (len = inputStream.read(buffer)) > 0) 
                    {
                    	baos.write(buffer,0,len);
                    } 


                return baos.toByteArray();
            }
            catch (Exception e)
            {
                throw new TransformerException(this, e);
            }
            finally
            {
                IOUtils.closeQuietly(inputStream);
            }


        } 
        else
        {
            throw new TransformerException(Message.createStaticMessage("Message is not an instance of java.io.InputStream"), this);
        }
    }
 
}