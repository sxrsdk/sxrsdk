/* Copyright 2016 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.samsungxr.x3d;

import com.samsungxr.SXRNode;

/**
 * 
 * @author m1.williams
 * Class of an array list of <Inline> objects
 * These are added to the Scene Graph after parsing
 * the original file.  
 */

public class InlineObject {

  private SXRNode inlineSXRNode = null;
  private String[] url = {};
  private boolean mLoad = true;
  private String mName = "";

  public InlineObject() { }

  public InlineObject(SXRNode inlineSXRNode, String[] url, String name, boolean load) {
     this.inlineSXRNode = inlineSXRNode;
     this.url = url;
      mLoad = load;
  }

  public String[] getURL() {
     return this.url;
  }

  public void setUrl(String url) {
      if (this.url != null) this.url[0] = url;
  }

    public void setUrl(String[] _url) {
      this.url = new String[_url.length];
      for (int i = 0; i < this.url.length; i++) {
          this.url[i] = _url[i];
      }
    }

  public int getTotalURL() {
     return url.length;
  }

  public SXRNode getInlineSXRNode() {
     return this.inlineSXRNode;
  }

    public boolean getLoad() {
        return mLoad;
    }

    public void setLoad( boolean load) {
        mLoad = load;
    }

}



