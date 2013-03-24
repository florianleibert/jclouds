/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WATCANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.ultradns.ws.features;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.logging.Logger.getAnonymousLogger;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.jclouds.rest.ResourceNotFoundException;
import org.jclouds.ultradns.ws.UltraDNSWSExceptions.ResourceAlreadyExistsException;
import org.jclouds.ultradns.ws.domain.Account;
import org.jclouds.ultradns.ws.domain.TrafficControllerPool;
import org.jclouds.ultradns.ws.domain.TrafficControllerPoolRecord;
import org.jclouds.ultradns.ws.domain.TrafficControllerPoolRecord.Status;
import org.jclouds.ultradns.ws.domain.Zone;
import org.jclouds.ultradns.ws.internal.BaseUltraDNSWSApiLiveTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

/**
 * @author Adrian Cole
 */
@Test(groups = "live", singleThreaded = true, testName = "TrafficControllerPoolApiLiveTest")
public class TrafficControllerPoolApiLiveTest extends BaseUltraDNSWSApiLiveTest {

   private String zoneName = System.getProperty("user.name").replace('.', '-') + ".tcpool.ultradnstest.jclouds.org.";
   private Account account;

   @Override
   @BeforeClass(groups = { "integration", "live" })
   public void setupContext() {
      super.setupContext();
      context.getApi().getZoneApi().delete(zoneName);
      account = context.getApi().getCurrentAccount();
      context.getApi().getZoneApi().createInAccount(zoneName, account.getId());
   }

   private void checkTCPool(TrafficControllerPool pool) {
      checkNotNull(pool.getZoneId(), "ZoneId cannot be null for  %s", pool);
      checkNotNull(pool.getId(), "Id cannot be null for  %s", pool);
      checkNotNull(pool.getName(), "Name cannot be null for  %s", pool);
      checkNotNull(pool.getDName(), "DName cannot be null for  %s", pool);
   }

   @Test
   public void testListTCPools() {
      for (Zone zone : context.getApi().getZoneApi().listByAccount(account.getId())) {
         for (TrafficControllerPool pool : api(zone.getName()).list()) {
            checkTCPool(pool);
         }
      }
   }

   @Test
   public void testListTCPoolRecords() {
      for (Zone zone : context.getApi().getZoneApi().listByAccount(account.getId())) {
         for (TrafficControllerPool pool : api(zone.getName()).list()) {
            for (TrafficControllerPoolRecord record : api(zone.getName()).listRecords(pool.getId())) {
               checkTrafficControllerPoolRecord(record);
            }
         }
      }
   }

   static void checkTrafficControllerPoolRecord(TrafficControllerPoolRecord record) {
      checkNotNull(record.getId(), "Id cannot be null for %s", record);
      checkNotNull(record.getPoolId(), "PoolId cannot be null for %s", record);
      checkNotNull(record.getPointsTo(), "PointsTo cannot be null for %s", record);
      assertTrue(record.getWeight() >= 0, "Weight must be unsigned for " + record);
      assertTrue(record.getPriority() >= 0, "Priority must be unsigned for " + record);
      checkNotNull(record.getType(), "Type cannot be null for %s", record);
      checkNotNull(record.getStatus(), "Status cannot be null for %s", record);
      assertTrue(record.getStatus() != Status.UNRECOGNIZED, "unrecognized status for " + record);
      checkNotNull(record.getDescription(), "Description cannot be null for %s", record);
   }

   @Test(expectedExceptions = ResourceNotFoundException.class, expectedExceptionsMessageRegExp = "Zone does not exist in the system.")
   public void testListTCPoolsWhenZoneIdNotFound() {
      api("AAAAAAAAAAAAAAAA").list();
   }

   @Test
   public void testDeleteWhenNotFound() {
      api(zoneName).delete("06063D9C54C5AE09");
   }

   String hostname = "www.tcpool." + zoneName;
   String poolId;

   @Test
   public void testCreatePool() {
      poolId = api(zoneName).createPoolForHostname("pool", hostname);
      getAnonymousLogger().info("created tc pool: " + poolId);
      try {
         api(zoneName).createPoolForHostname("pool", hostname);
         fail();
      } catch (ResourceAlreadyExistsException e) {

      }
      Optional<TrafficControllerPool> pool = getPoolById(poolId);
      assertTrue(pool.isPresent());
      assertEquals(pool.get().getName(), "pool");
      assertEquals(pool.get().getDName(), hostname);
      checkTCPool(pool.get());
   }

   @Test(dependsOnMethods = "testCreatePool")
   public void testDeletePool() {
      api(zoneName).delete(poolId);
      assertFalse(getPoolById(poolId).isPresent());
   }

   protected Optional<TrafficControllerPool> getPoolById(final String poolId) {
      return api(zoneName).list().firstMatch(new Predicate<TrafficControllerPool>() {
         public boolean apply(TrafficControllerPool in) {
            return in.getId().equals(poolId);
         }
      });
   }

   private TrafficControllerPoolApi api(String zoneName) {
      return context.getApi().getTrafficControllerPoolApiForZone(zoneName);
   }

   @Override
   @AfterClass(groups = { "integration", "live" })
   protected void tearDownContext() {
      if (poolId != null)
         api(zoneName).delete(poolId);
      context.getApi().getZoneApi().delete(zoneName);
      super.tearDownContext();
   }
}