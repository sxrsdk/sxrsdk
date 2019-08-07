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

import com.samsungxr.io.SXRCursorController;

public class IODevice implements Comparable<IODevice>
{
    public int priority;
    private int vendorID;
    private int productID;
    private String deviceID;
    private String name;
    private SXRCursorController cursorController;


    IODevice(String deviceID, String name, int vendorid, int productid)
    {
        this.priority = 0;
        this.vendorID = vendorid;
        this.productID = productid;
        this.name = name;
        this.deviceID = deviceID;
        this.cursorController = null;
    }

    public SXRCursorController getController() { return cursorController; }

    public int getPriority() { return priority; }

    void setPriority(int p) { priority = p; }

    public int getVendorID() { return vendorID; }

    public int getProductID() { return productID; }

    public String getDeviceID() { return deviceID; }

    public String getName() { return name; }

    public void setController(SXRCursorController controller) { cursorController = controller; }

    @Override
    public int compareTo(IODevice another)
    {
        int priorityDiff = priority - another.getPriority();
        if (priorityDiff == 0)
        {
            return hashCode() - another.hashCode();
        }
        else
        {
            return priorityDiff;
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IODevice that = (IODevice) o;

        return  (priority == that.priority) &&
                (vendorID == that.vendorID) &&
                (productID == that.productID) &&
                (name == that.name) &&
                (deviceID == that.deviceID);
    }

    @Override
    public int hashCode()
    {
        return (vendorID << 24)  | (productID << 8) | priority;
    }
}