//* Licensed Materials - Property of                                  *
//* IBM                                                               *
//*                                                                   *
//* eu.abc4trust.pabce.1.34                                           *
//*                                                                   *
//* (C) Copyright IBM Corp. 2014. All Rights Reserved.                *
//* US Government Users Restricted Rights - Use, duplication or       *
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. *
//*                                                                   *
//* This file is licensed under the Apache License, Version 2.0 (the  *
//* "License"); you may not use this file except in compliance with   *
//* the License. You may obtain a copy of the License at:             *
//*   http://www.apache.org/licenses/LICENSE-2.0                      *
//* Unless required by applicable law or agreed to in writing,        *
//* software distributed under the License is distributed on an       *
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY            *
//* KIND, either express or implied.  See the License for the         *
//* specific language governing permissions and limitations           *
//* under the License.                                                *
//*/**/****************************************************************

package io.exonym.abc.attributeType;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class EnumAllowedValuesWithIndexer extends EnumAllowedValues {
  private final EnumIndexer indexer;
  
  public EnumAllowedValuesWithIndexer(EnumIndexer indexer, List<String> allowedValues) {
    super(allowedValues);
    this.indexer = indexer;
  }
  
  public Map<String, BigInteger> getEncodingForEachAllowedValue() {
    Map<String, BigInteger> res = new HashMap<String, BigInteger>();
    List<String> list = getAllowedValues();
    for(int i=0;i<list.size();++i) {
      res.put(list.get(i), indexer.getRepresentationOfIndex(i));
    }
    return res;
  }
}
