
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

import com.samsungxr.animation.keyframe.GVRNodeAnimation;


/**
 * 
 * @author m1.williams
 * Handles those <ROUTE>s that setup animations as opposed to Sensors
 * such as TouchSensors.
 * When this <ROUTE> is accessed in the parser and during the animation
 * loop, we have access to turning animations off and on.
 */

public class RouteAnimation extends Route {

   // If a ROUTE is associated with GVRNodeAnimation, then
   // this value will point to it.
   // Assists with Touch Sensors
   private GVRNodeAnimation gvrAnimation = null;

  public RouteAnimation(String fromNode, String fromField, String toNode, String toField) {
    super(fromNode, fromField, toNode, toField);
  }


  public void setAnimation(GVRNodeAnimation gvrKeyFrameAnimation) {
  	this.gvrAnimation = gvrKeyFrameAnimation;
  }

  public GVRNodeAnimation getAnimation() {
  	return this.gvrAnimation;
  }

}


