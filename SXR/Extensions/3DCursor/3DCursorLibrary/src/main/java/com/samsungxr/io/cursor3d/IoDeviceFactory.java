/*
 * Copyright (c) 2016. Samsung Electronics Co., LTD
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.samsungxr.io.cursor3d;

import android.view.InputDevice;

import com.samsungxr.utility.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;

class IoDeviceFactory {
    private static final String TAG = IoDeviceFactory.class.getSimpleName();
    private static final String VENDOR_ID = "vendorId";
    private static final String PRODUCT_ID = "productId";
    private static final String DEVICE_ID = "deviceId";
    private static final String PRIORITY = "priority";
    private static final String VENDOR_NAME = "vendorName";
    private static final String NAME = "name";
    private static final String XML_START_TAG = "<io ";
    static final int INVALID_PRIORITY = -1;

    static IODevice readIoDeviceFromSettingsXml(XmlPullParser parser) throws
            XmlPullParserException, IOException
    {
        int priority;
        IODevice device = null;
        try
        {
            device = readIoDevice(parser);
            priority = Integer.parseInt(parser.getAttributeValue(XMLUtils.DEFAULT_XML_NAMESPACE, PRIORITY));
            device.setPriority(priority);
        }
        catch (NumberFormatException e)
        {
            throw new XmlPullParserException("Invalid VendorId, ProductId or Priority for Io " + "device");
        }
        XMLUtils.parseTillElementEnd(parser);
        return device;
    }

    private static IODevice readIoDevice(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        int vendorID;
        int productID;
        String deviceID;
        String name;
        try
        {
            vendorID = Integer.parseInt(parser.getAttributeValue(XMLUtils.DEFAULT_XML_NAMESPACE, VENDOR_ID));
            productID = Integer.parseInt(parser.getAttributeValue(XMLUtils.DEFAULT_XML_NAMESPACE, PRODUCT_ID));
        }
        catch (NumberFormatException e)
        {
            throw new XmlPullParserException("Invalid VendorId, ProductId or Priority for Io " +
                    "device");
        }

        deviceID = parser.getAttributeValue(XMLUtils.DEFAULT_XML_NAMESPACE, DEVICE_ID);
        if (deviceID == null)
        {
            throw new XmlPullParserException("deviceId for cursors IO device not specified");
        }
        parser.getAttributeValue(XMLUtils.DEFAULT_XML_NAMESPACE, VENDOR_NAME);
        name = parser.getAttributeValue(XMLUtils.DEFAULT_XML_NAMESPACE, NAME);
        return new IODevice(deviceID, name, vendorID, productID);
    }

    static IODevice readIoDeviceFromIoXml(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        IODevice ioDevice = readIoDevice(parser);
        XMLUtils.parseTillElementEnd(parser);
        return ioDevice;
    }

    //TODO use XmlSerializer
    static void writeIoDevice(IODevice device, BufferedWriter writer, int priority) throws IOException
    {
        writer.write(XML_START_TAG);
        XMLUtils.writeXmlAttribute(VENDOR_ID, device.getVendorID(), writer);
        XMLUtils.writeXmlAttribute(PRODUCT_ID, device.getProductID(), writer);
        XMLUtils.writeXmlAttribute(DEVICE_ID, device.getDeviceID(), writer);

        String name = device.getName();
        if (name != null) {
            XMLUtils.writeXmlAttribute(NAME, name, writer);
        }

        if (priority != INVALID_PRIORITY) {
            XMLUtils.writeXmlAttribute(PRIORITY, priority, writer);
        }
        writer.write(XMLUtils.ELEMENT_END);
    }

    static String getXmlString(IODevice device)
    {
        StringWriter stringWriter = new StringWriter();
        BufferedWriter writer = new BufferedWriter(stringWriter);
        try
        {
            writeIoDevice(device, writer, INVALID_PRIORITY);
            writer.flush();
        }
        catch (IOException e)
        {
            Log.d(TAG, "Cannot convert IoDevice to string:", e);
        }
        return stringWriter.toString();
    }
}
