/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
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

package net.fabricmc.installer.server;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.fabricmc.installer.Handler;
import net.fabricmc.installer.InstallerGui;
import net.fabricmc.installer.LoaderVersion;
import net.fabricmc.installer.Main;
import net.fabricmc.installer.util.ArgumentParser;
import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;

public class ServerHandler extends Handler {
	@Override
	public String name() {
		return "Server";
	}

	@Override
	public void install() {
		String gameVersion = Main.MINECRAFT_VERSION;
		LoaderVersion loaderVersion = Main.LOADER_VERSION;
		if (loaderVersion == null) return;

		new Thread(() -> {
			try {
				InputStream zipInputStream = Main.class.getClassLoader().getResourceAsStream(Main.SERVER_ZIP_NAME + "-" + loaderVersion.name + ".zip");

				Files.createDirectories(Paths.get(installLocation.getText()).toAbsolutePath());

				String zipFileName = Main.SERVER_ZIP_NAME + "-" + loaderVersion.name + ".zip";

				Path zipFilePath = Paths.get(installLocation.getText()).toAbsolutePath().resolve(zipFileName);

				Files.copy(zipInputStream, zipFilePath, StandardCopyOption.REPLACE_EXISTING);

				ServerInstaller.installFromZip(Paths.get(installLocation.getText()).toAbsolutePath(), zipFilePath, this);

				Files.delete(zipFilePath);

				ServerPostInstallDialog.show(this);
			} catch (Exception e) {
				error(e);
			}

			buttonInstall.setEnabled(true);
		}).start();
	}

	@Override
	public void installCli(ArgumentParser args) throws Exception {
		Path dir = Paths.get(args.getOrDefault("dir", () -> ".")).toAbsolutePath().normalize();

		if (!Files.isDirectory(dir)) {
			throw new FileNotFoundException("Server directory not found at " + dir + " or not a directory");
		}

		LoaderVersion loaderVersion = new LoaderVersion(getLoaderVersion(args));
		String gameVersion = getGameVersion(args);
		ServerInstaller.install(dir, loaderVersion, gameVersion, InstallerProgress.CONSOLE);

		if (args.has("downloadMinecraft")) {
			InstallerProgress.CONSOLE.updateProgress(Utils.BUNDLE.getString("progress.download.minecraft"));
			Path serverJar = dir.resolve("server.jar");
			MinecraftServerDownloader downloader = new MinecraftServerDownloader(gameVersion);
			downloader.downloadMinecraftServer(serverJar);
			InstallerProgress.CONSOLE.updateProgress(Utils.BUNDLE.getString("progress.done"));
		}

		InstallerProgress.CONSOLE.updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.done.start.server")).format(new Object[]{ServerInstaller.DEFAULT_LAUNCH_JAR_NAME}));
	}

	@Override
	public String cliHelp() {
		return "-dir <install dir, default current dir> -mcversion <minecraft version, default latest> -loader <loader version, default latest> -downloadMinecraft";
	}

	@Override
	public void setupPane1(JPanel pane, GridBagConstraints c, InstallerGui installerGui) {
		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			return;
		}

		JLabel label = new JLabel(String.format("<html><a href=\"\">%s</a></html>", Utils.BUNDLE.getString("prompt.server.launcher")));
		label.setCursor(new Cursor(Cursor.HAND_CURSOR));
		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					Desktop.getDesktop().browse(new URI(Reference.SERVER_LAUNCHER_URL));
				} catch (IOException | URISyntaxException ex) {
					ex.printStackTrace();
				}
			}
		});

		addRow(pane, c, null, label);
	}

	@Override
	public void setupPane2(JPanel pane, GridBagConstraints c, InstallerGui installerGui) {
		installLocation.setText(Paths.get(".").toAbsolutePath().normalize().toString());
	}
}
