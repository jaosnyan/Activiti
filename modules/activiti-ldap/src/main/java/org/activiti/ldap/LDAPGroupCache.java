/* Licensed under the Apache License, Version 2.0 (the "License");
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
package org.activiti.ldap;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.identity.Group;
import org.activiti.engine.impl.util.ClockUtil;


/**
 * @author Joram Barrez
 */
public class LDAPGroupCache {
  
  protected Map<String, LDAPGroupCacheEntry> groupCache;
  protected long expirationTime;
  
  protected LDAPGroupCacheListener ldapCacheListener;
  
  public LDAPGroupCache(final int cacheSize, final long expirationTime) {
    
    // From http://stackoverflow.com/questions/224868/easy-simple-to-use-lru-cache-in-java
    this.groupCache =new LinkedHashMap<String, LDAPGroupCache.LDAPGroupCacheEntry>(cacheSize + 1, 0.75f, true) {

      private static final long serialVersionUID = 5207574193173514579L;

      protected boolean removeEldestEntry(java.util.Map.Entry<String, LDAPGroupCacheEntry> eldest) {
        boolean removeEldest = size() > cacheSize;
        
        if (removeEldest && ldapCacheListener != null) {
          ldapCacheListener.cacheEviction(eldest.getKey());
        }
        
        return removeEldest;
      }
      
    };
    this.expirationTime = expirationTime;
  }
  
  public void add(String userId, List<Group> groups) {
    this.groupCache.put(userId, new LDAPGroupCacheEntry(ClockUtil.getCurrentTime(), groups));
  }
  
  public List<Group> get(String userId) {
    LDAPGroupCacheEntry cacheEntry = groupCache.get(userId);
    if (cacheEntry != null) {
      if ((ClockUtil.getCurrentTime().getTime() - cacheEntry.getTimestamp().getTime()) < expirationTime) {
        
        if (ldapCacheListener != null) {
          ldapCacheListener.cacheHit(userId);
        }
        
        return cacheEntry.getGroups();
        
      } else {
        
        this.groupCache.remove(userId);
        
        if (ldapCacheListener != null) {
          ldapCacheListener.cacheExpired(userId);
          ldapCacheListener.cacheEviction(userId);
        }
        
      }
    }
    
    if (ldapCacheListener != null) {
      ldapCacheListener.cacheMiss(userId);
    }
    
    return null;
  }
  
  public void clear() {
    groupCache.clear();
  }
  
  public Map<String, LDAPGroupCacheEntry> getGroupCache() {
    return groupCache;
  }

  public void setGroupCache(Map<String, LDAPGroupCacheEntry> groupCache) {
    this.groupCache = groupCache;
  }
  
  public long getExpirationTime() {
    return expirationTime;
  }
  
  public void setExpirationTime(long expirationTime) {
    this.expirationTime = expirationTime;
  }
  
  public LDAPGroupCacheListener getLdapCacheListener() {
    return ldapCacheListener;
  }
  
  public void setLdapCacheListener(LDAPGroupCacheListener ldapCacheListener) {
    this.ldapCacheListener = ldapCacheListener;
  }
  
  
  // Helper classes ////////////////////////////////////

  static class LDAPGroupCacheEntry {
    
    protected Date timestamp;
    protected List<Group> groups;
    
    public LDAPGroupCacheEntry() {
      
    }
    
    public LDAPGroupCacheEntry(Date timestamp, List<Group> groups) {
      this.timestamp = timestamp;
      this.groups = groups;
    }
    
    public Date getTimestamp() {
      return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
      this.timestamp = timestamp;
    }
    
    public List<Group> getGroups() {
      return groups;
    }
    
    public void setGroups(List<Group> groups) {
      this.groups = groups;
    }
    
  }
  
  // Cache listeners. Currently not yet exposed (only programmatically for the moment)
  
  // Experimental stuff!
  
  public static interface LDAPGroupCacheListener {
    
    void cacheHit(String userId);
    void cacheMiss(String userId);
    void cacheEviction(String userId);
    void cacheExpired(String userId);
    
  }

}
