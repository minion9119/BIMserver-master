package org.bimserver.tests.serviceinterface;

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.commons.io.IOUtils;
import org.bimserver.database.queries.om.DefaultQueries;
import org.bimserver.interfaces.objects.SActionState;
import org.bimserver.interfaces.objects.SDeserializerPluginConfiguration;
import org.bimserver.interfaces.objects.SLongActionState;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.interfaces.objects.SSerializerPluginConfiguration;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.plugins.services.Flow;
import org.bimserver.shared.UsernamePasswordAuthenticationInfo;
import org.bimserver.test.TestWithEmbeddedServer;
import org.junit.Test;

public class SingleCheckinAndDownload extends TestWithEmbeddedServer {

	@Test
	public void test() {
		try {
			// Create a new BimServerClient with authentication
			BimServerClientInterface bimServerClient = getFactory().create(new UsernamePasswordAuthenticationInfo("admin@bimserver.org", "admin"));
			
			// When you use the channel for checkin/download calls, all data will have to be converted to the used channel's format, for example for JSON, binary data will be Base64 encoded, which will make things slower and larger
			// The alternative is to use the Servlets, those will also use compression where possible
			boolean useChannel = false; // Using the channel is slower

			// Create a new project
			SProject newProject = bimServerClient.getServiceInterface().addProject("test" + Math.random(), "ifc2x3tc1");
			
			// This is the file we will be checking in
			Path ifcFile = Paths.get("../TestData/data/AC11-FZK-Haus-IFC.ifc");
			
			// Find a deserializer to use
			SDeserializerPluginConfiguration deserializer = bimServerClient.getServiceInterface().getSuggestedDeserializerForExtension("ifc", newProject.getOid());
			
			// Checkin
			Long progressId = -1L;
			if (useChannel) {
				progressId = bimServerClient.getServiceInterface().checkin(newProject.getOid(), "test", deserializer.getOid(), ifcFile.toFile().length(), ifcFile.getFileName().toString(), new DataHandler(new FileDataSource(ifcFile.toFile())), true, true);
			} else {
				progressId = bimServerClient.checkin(newProject.getOid(), "test", deserializer.getOid(), false, Flow.SYNC, ifcFile);
			}
			
			// Get the status
			SLongActionState longActionState = bimServerClient.getRegistry().getProgress(progressId);
			if (longActionState.getState() == SActionState.FINISHED) {
				// Find a serializer
				SSerializerPluginConfiguration serializer = bimServerClient.getServiceInterface().getSerializerByContentType("application/ifc");
				
				// Get the project details
				newProject = bimServerClient.getServiceInterface().getProjectByPoid(newProject.getOid());
				
				// Download the latest revision  (the one we just checked in)
				if (useChannel) {
					Long topicId = bimServerClient.getServiceInterface().download(Collections.singleton(newProject.getLastRevisionId()), DefaultQueries.allAsString(), serializer.getOid(), true);
					SLongActionState downloadState = bimServerClient.getRegistry().getProgress(topicId);
					if (downloadState.getState() == SActionState.FINISHED) {
						InputStream inputStream = bimServerClient.getServiceInterface().getDownloadData(topicId).getFile().getInputStream();
						IOUtils.copy(inputStream, new ByteArrayOutputStream());
						System.out.println("Success");
					}
				} else {
					Long topicId = bimServerClient.getServiceInterface().download(Collections.singleton(newProject.getLastRevisionId()), DefaultQueries.allAsString(), serializer.getOid(), false); // Note: sync: false
					InputStream downloadData = bimServerClient.getDownloadData(topicId);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					IOUtils.copy(downloadData, baos);
					System.out.println(baos.size() + " bytes downloaded");
				}
			} else {
				System.out.println(longActionState.getState());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}