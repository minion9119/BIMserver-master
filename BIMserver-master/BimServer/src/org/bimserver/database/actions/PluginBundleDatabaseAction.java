package org.bimserver.database.actions;

/******************************************************************************
 * Copyright (C) 2009-2017  BIMserver.org
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see {@literal<http://www.gnu.org/licenses/>}.
 *****************************************************************************/

import java.io.IOException;
import java.util.GregorianCalendar;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.bimserver.database.DatabaseSession;
import org.bimserver.interfaces.objects.SPluginBundle;
import org.bimserver.interfaces.objects.SPluginBundleType;
import org.bimserver.interfaces.objects.SPluginBundleVersion;
import org.bimserver.models.log.AccessMethod;
import org.bimserver.plugins.MavenDependency;
import org.bimserver.plugins.MavenPluginLocation;
import org.bimserver.plugins.MavenPluginVersion;
import org.bimserver.plugins.PluginLocation;
import org.bimserver.plugins.PluginVersion;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PluginBundleDatabaseAction<T> extends BimDatabaseAction<T>{
	private static final Logger LOGGER = LoggerFactory.getLogger(PluginBundleDatabaseAction.class);
	
	public PluginBundleDatabaseAction(DatabaseSession databaseSession, AccessMethod accessMethod) {
		super(databaseSession, accessMethod);
	}
	

	public SPluginBundle processPluginLocation(PluginLocation<?> pluginLocation, boolean strictVersionChecking, DefaultArtifactVersion bimserverVersion) {
		if (pluginLocation instanceof MavenPluginLocation) {
			return processMavenPluginLocation((MavenPluginLocation) pluginLocation, strictVersionChecking, bimserverVersion);
		}
		return null;
	}
	
	public SPluginBundle processMavenPluginLocation(MavenPluginLocation mavenPluginLocation, boolean strictVersionChecking, ArtifactVersion bimserverVersion) {
		SPluginBundle pluginBundle = new SPluginBundle();
		boolean usefulBundle = false;
		for (PluginVersion pluginVersion : mavenPluginLocation.getAllVersions()) {
			if (pluginVersion instanceof MavenPluginVersion) {
				SPluginBundleVersion sPluginBundleVersion = new SPluginBundleVersion();
				boolean useful = true;
				MavenPluginVersion mavenPluginVersion = (MavenPluginVersion) pluginVersion;
				for (MavenDependency mavenDependency : mavenPluginVersion.getDependencies()) {
					if (mavenDependency.getArtifact().getGroupId().equals("org.opensourcebim")) {
						String artifactId = mavenDependency.getArtifact().getArtifactId();
						// shared and pluginbase always have the same version
						// as this BIMserver, so if any of them is a dependency
						// for the plugin, it's version has to be ok
						if (artifactId.equals("shared") || artifactId.equals("pluginbase")) {
							VersionRange versionRange = VersionRange.createFromVersion(mavenDependency.getArtifact().getVersion());
							if (bimserverVersion != null && versionRange.containsVersion(bimserverVersion)) {

							} else {
								sPluginBundleVersion.setMismatch(true);
								if (strictVersionChecking) {
									useful = false;
									LOGGER.info("Skipping version " + mavenPluginVersion.getArtifact().getVersion() + " or artifact " + mavenPluginVersion.getArtifact().getArtifactId());
								}
							}
						}
					}
				}
				if (useful) {
					usefulBundle = true;

					sPluginBundleVersion.setName(mavenPluginVersion.getModel().getName());
					sPluginBundleVersion.setOrganization(mavenPluginVersion.getModel().getOrganization().getName());
					sPluginBundleVersion.setArtifactId(mavenPluginLocation.getArtifactId());
					sPluginBundleVersion.setGroupId(mavenPluginLocation.getGroupId());
					try {
						sPluginBundleVersion.setRepository(mavenPluginLocation.getRepository(mavenPluginVersion.getVersion().toString()));
					} catch (ArtifactResolutionException e) {
						LOGGER.error("", e);
					}
					sPluginBundleVersion.setType(SPluginBundleType.MAVEN);
					sPluginBundleVersion.setVersion(mavenPluginVersion.getVersion().toString());
					sPluginBundleVersion.setDescription(mavenPluginVersion.getModel().getDescription());
					
					pluginBundle.setName(mavenPluginVersion.getModel().getName());
					pluginBundle.setOrganization(mavenPluginVersion.getModel().getOrganization().getName());
					pluginBundle.setLatestVersion(sPluginBundleVersion);
					pluginBundle.getAvailableVersions().add(sPluginBundleVersion);

					try {
						sPluginBundleVersion.setIcon(mavenPluginLocation.getVersionIcon(mavenPluginVersion.getVersion().toString()));
					} catch (ArtifactResolutionException e) {
						// This is not important
					} catch (IOException e) {
						LOGGER.error("", e);
					}
					
					try {
						GregorianCalendar date = mavenPluginLocation.getVersionDate(mavenPluginVersion.getVersion().toString());
						if (date != null) {
							sPluginBundleVersion.setDate(date.getTime());
						}
//						byte[] bytes = Files.readAllBytes(date);
//						Properties properties = new Properties();
//						properties.load(new ByteArrayInputStream(bytes));
//						String buildDateString = properties.getProperty("build.date");
//						
//						DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
//						try {
//						} catch (ParseException e) {
//							LOGGER.error("Invalid date format for plugin " + mavenPluginVersion.getModel().getName() + ": '" + buildDateString + "'");
//						}
					} catch (ArtifactResolutionException e) {
						// Not a problem
					} catch (Exception e) {
						LOGGER.error("", e);
					}
				}
			}
		}
		if (usefulBundle) {
			return pluginBundle;
		}
		return null;
	}
}
