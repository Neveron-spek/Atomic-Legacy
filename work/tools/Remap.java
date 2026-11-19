import javax.imageio.ImageIO; import java.awt.image.BufferedImage; import java.io.File;
public class Remap {
  static int c(int col,int s){return (col>>s)&0xff;}
  static int shift(int pix,int tl,int td){int sl=0xFFFFFF,sd=0x505050;
    int a=(pix&0xff000000)>>24; int[] v={(pix&0xff0000)>>16,(pix&0xff00)>>8,pix&0xff}; int[] sh={16,8,0}; int[] o=new int[3];
    for(int i=0;i<3;i++){double pos=(v[i]-(double)c(sl,sh[i]))/((double)c(sd,sh[i])-c(sl,sh[i])); o[i]=((int)(c(tl,sh[i])+pos*(c(td,sh[i])-(double)c(tl,sh[i]))))&0xff;}
    return (a<<24)|(o[0]<<16)|(o[1]<<8)|o[2];}
  public static void main(String[] a) throws Exception {
    if(a[0].equals("cmp")){BufferedImage x=ImageIO.read(new File(a[1])),y=ImageIO.read(new File(a[2]));int d=0;for(int i=0;i<x.getWidth();i++)for(int j=0;j<x.getHeight();j++)if(x.getRGB(i,j)!=y.getRGB(i,j)&&((y.getRGB(i,j)>>>24)>0||(x.getRGB(i,j)>>>24)>0))d++;System.out.println("diff="+d);return;}
    // batch: src dst tl td (repeated groups of 4)
    for(int k=0;k+3<a.length;k+=4){BufferedImage im=ImageIO.read(new File(a[k]));BufferedImage o=new BufferedImage(im.getWidth(),im.getHeight(),BufferedImage.TYPE_INT_ARGB);
      int tl=Integer.parseInt(a[k+2],16),td=Integer.parseInt(a[k+3],16);
      for(int i=0;i<im.getWidth();i++)for(int j=0;j<im.getHeight();j++)o.setRGB(i,j,shift(im.getRGB(i,j),tl,td));
      ImageIO.write(o,"png",new File(a[k+1]));}
  }
}
