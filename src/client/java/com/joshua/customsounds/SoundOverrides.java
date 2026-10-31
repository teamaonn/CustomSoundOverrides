package com.joshua.customsounds;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import org.slf4j.*;

/** A generated overlay pack reads user-supplied files; original assets are never modified. */
public final class SoundOverrides implements RepositorySource {
 private static final Logger LOG=LoggerFactory.getLogger("custom_sounds");
 private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
 public static Identifier eventFor(String filename){
  if(!filename.endsWith(".ogg"))return null;
  String name=filename.substring(0,filename.length()-4);int split=name.indexOf("__");
  return Identifier.tryParse(split<0?"minecraft:"+name:name.substring(0,split)+":"+name.substring(split+2));
 }
 public static boolean vorbis(Path file)throws IOException{
  try(InputStream in=Files.newInputStream(file)){
   byte[] h=in.readNBytes(27);if(h.length!=27||h[0]!='O'||h[1]!='g'||h[2]!='g'||h[3]!='S'||h[4]!=0)return false;
   int n=h[26]&255;if(n==0)return false;byte[] segments=in.readNBytes(n);if(segments.length!=n)return false;
   byte[] prefix=in.readNBytes(7);if (!Arrays.equals(prefix,new byte[]{1,'v','o','r','b','i','s'})) return false;
  }
  try(var decoder=new net.minecraft.client.sounds.JOrbisAudioStream(Files.newInputStream(file))) {
   return decoder.getFormat().getChannels() > 0 && decoder.readChunk(sample -> {});
  } catch (IOException | RuntimeException invalid) { return false; }
 }
 public static Path rebuild(Path folder)throws IOException{
  Files.createDirectories(folder);Path generated=folder.resolve(".generated");
  if(Files.isSymbolicLink(generated))throw new IOException("Generated sound directory must not be a symbolic link");
  if(Files.exists(generated))try(var paths=Files.walk(generated)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}
  Files.createDirectories(generated);Map<String,JsonObject> namespaces=new TreeMap<>();
  try(var paths=Files.list(folder)){
   for(Path file:paths.filter(p->Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)&&p.getFileName().toString().endsWith(".ogg")).sorted().toList()){
    Identifier event=eventFor(file.getFileName().toString());
    if(event==null||BuiltInRegistries.SOUND_EVENT.get(event).isEmpty()){LOG.warn("Skipping unknown sound event file {}",file.getFileName());continue;}
    if(!vorbis(file)){LOG.warn("Skipping invalid Ogg Vorbis file {}",file.getFileName());continue;}
    String soundPath="custom_overrides/"+event.getPath();Path audio=generated.resolve("assets").resolve(event.getNamespace()).resolve("sounds").resolve(soundPath+".ogg");
    Files.createDirectories(audio.getParent());Files.copy(file,audio,StandardCopyOption.REPLACE_EXISTING);
    JsonObject replacement=new JsonObject();replacement.addProperty("replace",true);JsonArray sounds=new JsonArray();sounds.add(event.getNamespace()+":"+soundPath);replacement.add("sounds",sounds);
    namespaces.computeIfAbsent(event.getNamespace(),n->new JsonObject()).add(event.getPath(),replacement);
   }
  }
  for(var ns:namespaces.entrySet()){
   Path json=generated.resolve("assets").resolve(ns.getKey()).resolve("sounds.json");Files.createDirectories(json.getParent());Files.writeString(json,GSON.toJson(ns.getValue()));
  }
  var format=SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES);
  JsonObject pack=new JsonObject();pack.addProperty("description","User-supplied sound replacements");
  JsonArray version=new JsonArray();version.add(format.major());version.add(format.minor());pack.add("min_format",version);pack.add("max_format",version.deepCopy());
  JsonObject meta=new JsonObject();meta.add("pack",pack);Files.writeString(generated.resolve("pack.mcmeta"),GSON.toJson(meta));
  LOG.info("Loaded {} custom sound replacements",namespaces.values().stream().mapToInt(JsonObject::size).sum());return generated;
 }
 @Override public void loadPacks(java.util.function.Consumer<Pack> consumer){
  try{
   Path folder=FabricLoader.getInstance().getConfigDir().resolve("customsounds");
   if(!Files.exists(folder.resolve("README.txt"))){Files.createDirectories(folder);Files.writeString(folder.resolve("README.txt"),"Supply Ogg Vorbis files named after sound events, e.g. entity.creeper.primed.ogg. For mods use namespace__event.name.ogg. Reload with F3+T. Remove a file and reload to restore the original sound. No audio is bundled.\n");}
   Path generated=rebuild(folder);
   var location=new PackLocationInfo("custom_sounds/overrides",Component.literal("Custom Sounds"),PackSource.BUILT_IN,Optional.empty());
   Pack pack=Pack.readMetaAndCreate(location,new PathPackResources.PathResourcesSupplier(generated),PackType.CLIENT_RESOURCES,new PackSelectionConfig(true,Pack.Position.TOP,true));
   if(pack!=null)consumer.accept(pack);
  }catch(Exception e){LOG.error("Could not load custom sounds; original resources remain available",e);}
 }
}
