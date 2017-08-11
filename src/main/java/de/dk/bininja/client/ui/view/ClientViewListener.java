package de.dk.bininja.client.ui.view;

/**
 * @author David Koettlitz
 * <br>Erstellt am 07.08.2017
 */
public interface ClientViewListener extends DownloadViewListener {
   public void connectTo(String host, int port);
}
