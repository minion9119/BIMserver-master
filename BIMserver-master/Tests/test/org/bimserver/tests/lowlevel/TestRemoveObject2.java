package org.bimserver.tests.lowlevel;

import static org.junit.Assert.fail;

import java.nio.file.Paths;

import org.bimserver.interfaces.objects.SProject;
import org.bimserver.interfaces.objects.SSerializerPluginConfiguration;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.shared.UsernamePasswordAuthenticationInfo;
import org.bimserver.shared.interfaces.LowLevelInterface;
import org.bimserver.test.TestWithEmbeddedServer;
import org.junit.Test;

public class TestRemoveObject2 extends TestWithEmbeddedServer {

	@Test
	public void test() {
		try {
			// Create a new BimServerClient with authentication
			BimServerClientInterface bimServerClient = getFactory().create(new UsernamePasswordAuthenticationInfo("admin@bimserver.org", "admin"));
			
			// Get the service interface
			SSerializerPluginConfiguration serializer = bimServerClient.getServiceInterface().getSerializerByContentType("application/ifc");
			
			LowLevelInterface lowLevelInterface = bimServerClient.getLowLevelInterface();
			
			// Create a new project
			SProject newProject = bimServerClient.getServiceInterface().addProject("test" + Math.random(), "ifc2x3tc1");
			
			// Start a transaction
			Long tid = lowLevelInterface.startTransaction(newProject.getOid());
			
			Long ifcRelContainedInSpatialStructureOid = lowLevelInterface.createObject(tid, "IfcRelContainedInSpatialStructure", true);
			Long ifcBuildingOid = lowLevelInterface.createObject(tid, "IfcBuilding", true);
			Long ifcWallOid = lowLevelInterface.createObject(tid, "IfcWall", true);
			lowLevelInterface.addReference(tid, ifcBuildingOid, "ContainsElements", ifcRelContainedInSpatialStructureOid);
			lowLevelInterface.addReference(tid, ifcRelContainedInSpatialStructureOid, "RelatedElements", ifcWallOid);
			
			Long newRoid = lowLevelInterface.commitTransaction(tid, "Initial");

			bimServerClient.download(newRoid, serializer.getOid(), Paths.get("test1.ifc"));
			
			tid = lowLevelInterface.startTransaction(newProject.getOid());
			lowLevelInterface.removeObject(tid, ifcWallOid);
			lowLevelInterface.removeObject(tid, ifcRelContainedInSpatialStructureOid);
			newRoid = lowLevelInterface.commitTransaction(tid, "removed");
			
			bimServerClient.download(newRoid, serializer.getOid(), Paths.get("test2.ifc"));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}