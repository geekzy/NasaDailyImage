package net.geekzy.mobile.sax;

/**
 * IotdHandlerListener
 * 
 * @author Geroen Joris - http://www.headfirstandroid.com/
 */
public interface IotdHandlerListener {

	public void iotdParsed(String url, String title, String description,
			String date);

}
