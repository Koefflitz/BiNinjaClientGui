package de.dk.bininja.client.ui.view;

import java.net.URL;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public interface DownloadViewListener {
   public boolean requestDownloadFrom(URL url, DownloadView view);
}
