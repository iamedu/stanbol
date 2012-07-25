/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.commons.semanticindex.core.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndex;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndexManager;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Default implementation for {@link SemanticIndexManager}
 * 
 * @author suat
 * 
 */
@Component(immediate = true)
@Service
public class SemanticIndexManagerImpl implements SemanticIndexManager {
    
	/**
	 * Compares ServiceReferences based on their {@link Constants#SERVICE_RANKING} and
	 * {@link Constants#SERVICE_ID}. This is internally used to ensure that methods that
	 * return a single {@link SemanticIndex} do actually return the one with the highest
	 * priority.
	 */
    private static final Comparator<ServiceReference> SERVICE_REFERENCE_COMPARATOR = new Comparator<ServiceReference> (){
        
        @Override
        public int compare(ServiceReference ref1, ServiceReference ref2) {
            int r1,r2;
            Object tmp = ref1.getProperty(Constants.SERVICE_RANKING);
            r1 = tmp != null ? ((Integer)tmp).intValue() : 0;
            tmp = (Integer)ref2.getProperty(Constants.SERVICE_RANKING);
            r2 = tmp != null ? ((Integer)tmp).intValue() : 0;
            if(r1 == r2){
                tmp = (Long)ref1.getProperty(Constants.SERVICE_ID);
                long id1 = tmp != null ? ((Long)tmp).longValue() : Long.MAX_VALUE;
                tmp = (Long)ref2.getProperty(Constants.SERVICE_ID);
                long id2 = tmp != null ? ((Long)tmp).longValue() : Long.MAX_VALUE;
                //the lowest id must be first -> id1 < id2 -> [id1,id2] -> return -1
                return id1 < id2 ? -1 : id2 == id1 ? 0 : 1; 
            } else {
                //the highest ranking MUST BE first -> r1 < r2 -> [r2,r1] -> return 1
                return r1 < r2 ? 1:-1;
            }
        }
    };
    
    private ServiceTracker semanticIndexTracker;

    @Activate
    protected void activate(ComponentContext componentContext) {
        semanticIndexTracker = new ServiceTracker(componentContext.getBundleContext(),
                SemanticIndex.class.getName(), null);
        semanticIndexTracker.open();
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        if (semanticIndexTracker != null) {
            semanticIndexTracker.close();
        }
    }

    @Override
    public SemanticIndex<?> getIndex(String name) throws IndexManagementException {
        return getIndex(name, null);
    }

    @Override
    public List<SemanticIndex<?>> getIndexes(String name) throws IndexManagementException {
        return getIndexes(name, null);
    }

    @Override
    public SemanticIndex<?> getIndexByEndpointType(String endpointType) throws IndexManagementException {
        return getIndex(null, endpointType);
    }

    @Override
    public List<SemanticIndex<?>> getIndexesByEndpointType(String endpointType) throws IndexManagementException {
        return getIndexes(null, endpointType);
    }

    @Override
    public SemanticIndex<?> getIndex(String name, String endpointType) throws IndexManagementException {
        List<SemanticIndex<?>> result = getIndexList(name, endpointType, false);
        if (result.size() == 0) {
            return null;
        } else {
            return result.get(0);
        }

    }

    @Override
    public List<SemanticIndex<?>> getIndexes(String name, String endpointType) throws IndexManagementException {
        return getIndexList(name, endpointType, true);
    }

    private List<SemanticIndex<?>> getIndexList(String name, String endpointType, boolean multiple) {
        List<SemanticIndex<?>> results = new ArrayList<SemanticIndex<?>>();
        ServiceReference[] refs = semanticIndexTracker.getServiceReferences();
        if (refs != null) {
            if (refs.length > 1) {
                // TODO: rw move the ServiceReferenceRankingComperator to a utils module
                Arrays.sort(refs, SERVICE_REFERENCE_COMPARATOR);
            }
            for (ServiceReference ref : refs) {
                SemanticIndex<?> si = (SemanticIndex<?>) semanticIndexTracker.getService(ref);
                if (name == null || name.equals(si.getName())) {
                    if (endpointType == null) {
                        results.add(si);
                    } else {
                        // search both the RESTful and the JAVA interfaces
                        Set<String> endpointTypes = si.getRESTSearchEndpoints().keySet();
                        if (endpointTypes != null && endpointTypes.contains(endpointType)) {
                            results.add(si);
                        } else {
                            endpointTypes = si.getSearchEndPoints().keySet();
                            if (endpointTypes != null && endpointTypes.contains(endpointType)) {
                                results.add(si);
                            }
                        }
                    }
                }
                if (multiple == false && results.size() == 1) {
                    break;
                }
            }
        }
        return results;
    }
}
