package org.bimserver.tests.emf;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.emf.IfcModelInterfaceException;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.interfaces.objects.SSerializerPluginConfiguration;
import org.bimserver.models.ifc2x3tc1.IfcAxis2Placement3D;
import org.bimserver.models.ifc2x3tc1.IfcBuilding;
import org.bimserver.models.ifc2x3tc1.IfcBuildingStorey;
import org.bimserver.models.ifc2x3tc1.IfcElementCompositionEnum;
import org.bimserver.models.ifc2x3tc1.IfcFurnishingElement;
import org.bimserver.models.ifc2x3tc1.IfcInternalOrExternalEnum;
import org.bimserver.models.ifc2x3tc1.IfcLocalPlacement;
import org.bimserver.models.ifc2x3tc1.IfcRelAggregates;
import org.bimserver.models.ifc2x3tc1.IfcRepresentationContext;
import org.bimserver.models.ifc2x3tc1.IfcSpace;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.shared.UsernamePasswordAuthenticationInfo;
import org.bimserver.test.TestWithEmbeddedServer;
import org.bimserver.utils.RichIfcModel;
import org.junit.Test;

public class TestBigModelEmf extends TestWithEmbeddedServer {
	@Test
	public void test() {
		try {
			BimServerClientInterface bimServerClient = getFactory().create(new UsernamePasswordAuthenticationInfo("admin@bimserver.org", "admin"));
			
			SProject newProject = bimServerClient.getServiceInterface().addProject("test" + Math.random(), "ifc2x3tc1");
			
			IfcModelInterface model = bimServerClient.newModel(newProject, true);
			RichIfcModel richIfcModel = new RichIfcModel(model);
			
			IfcBuilding ifcBuilding = richIfcModel.createDefaultProjectStructure();
			
			IfcRelAggregates buildingAggregation = richIfcModel.create(IfcRelAggregates.class);
			buildingAggregation.setRelatingObject(ifcBuilding);
			for (int i=1; i<=10; i++) {
				IfcBuildingStorey ifcBuildingStorey = richIfcModel.create(IfcBuildingStorey.class);
				ifcBuildingStorey.setName("Storey " + i);
				ifcBuildingStorey.setCompositionType(IfcElementCompositionEnum.ELEMENT);
				ifcBuildingStorey.setElevation(3000 * i);
				
				IfcLocalPlacement storeyPlacement = richIfcModel.create(IfcLocalPlacement.class);
				storeyPlacement.setRelativePlacement(richIfcModel.createBasicPosition(0, 0, i * 3000));
				ifcBuildingStorey.setObjectPlacement(storeyPlacement);
				
				buildingAggregation.getRelatedObjects().add(ifcBuildingStorey);
				
				IfcRelAggregates storeyAggregation = richIfcModel.create(IfcRelAggregates.class);
				storeyAggregation.setRelatingObject(ifcBuildingStorey);
				
				for (int x=1; x<=10; x++) {
					for (int y=1; y<=10; y++) {
						createSpace(richIfcModel, richIfcModel.getDefaultRepresentationContext(), storeyPlacement, storeyAggregation, x, y);
					}
				}
			}
			
			long roid = model.commit("Initial model");

			SSerializerPluginConfiguration serializerByContentType = bimServerClient.getServiceInterface().getSerializerByName("Ifc2x3tc1 (Streaming)");
			bimServerClient.download(roid, serializerByContentType.getOid(), new FileOutputStream(new File("created.ifc")));
		} catch (Throwable e) {
			e.printStackTrace();
			if (e instanceof AssertionError) {
				throw (AssertionError)e;
			}
			fail(e.getMessage());
		}
	}

	private void createSpace(RichIfcModel richIfcModel, IfcRepresentationContext representationContext, IfcLocalPlacement storeyPlacement, IfcRelAggregates storeyAggregation, int x, int y) throws IfcModelInterfaceException {
		IfcSpace ifcSpace = richIfcModel.create(IfcSpace.class);
		ifcSpace.setName("Space " + ((y * 10) + x));
		ifcSpace.setCompositionType(IfcElementCompositionEnum.ELEMENT);
		ifcSpace.setInteriorOrExteriorSpace(IfcInternalOrExternalEnum.INTERNAL);

		IfcLocalPlacement spacePlacement = richIfcModel.create(IfcLocalPlacement.class);
		spacePlacement.setPlacementRelTo(storeyPlacement);
		spacePlacement.setRelativePlacement(richIfcModel.createBasicPosition(x * 6000, y * 6000, 0));
		ifcSpace.setObjectPlacement(spacePlacement);

		storeyAggregation.getRelatedObjects().add(ifcSpace);
		
		ifcSpace.setRepresentation(richIfcModel.createRectangularExtrusionProductRepresentation(5000, 5000, 2000));
		
		createFurnishing(richIfcModel, representationContext, y, ifcSpace, spacePlacement);
	}

	private void createFurnishing(RichIfcModel richIfcModel, IfcRepresentationContext representationContext, int number, IfcSpace ifcSpace, IfcLocalPlacement spacePlacement) throws IfcModelInterfaceException {
		IfcFurnishingElement furnishing = richIfcModel.create(IfcFurnishingElement.class);
		furnishing.setName("Furnishing " + number);
		
		IfcLocalPlacement furnishingPlacement = richIfcModel.create(IfcLocalPlacement.class);
		furnishingPlacement.setPlacementRelTo(spacePlacement);
		IfcAxis2Placement3D furnitureAxisPlacement = richIfcModel.create(IfcAxis2Placement3D.class);
		furnitureAxisPlacement.setLocation(richIfcModel.createIfcCartesianPoint(500, 500, 2));
		furnishingPlacement.setRelativePlacement(furnitureAxisPlacement);
		
		furnishing.setObjectPlacement(furnishingPlacement);
		furnishing.setRepresentation(richIfcModel.createRectangularExtrusionProductRepresentation(4000, 4000, 1200));
		
		richIfcModel.addContains(ifcSpace, furnishing);
	}
}
