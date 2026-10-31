package com.joshua.customsounds.mixin;
import java.util.*;
import net.minecraft.server.packs.repository.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.joshua.customsounds.SoundOverrides;
@Mixin(PackRepository.class)
public abstract class ClientPackRepositoryMixin {
 @Shadow @Final @Mutable private Set<RepositorySource> sources;
 @Inject(method="<init>",at=@At("RETURN"))
 private void custom_sounds$addSource(RepositorySource[] initial,CallbackInfo ci){
  // The mixin is client-only. Only repositories containing the vanilla client resources receive overrides.
  if(Arrays.stream(initial).noneMatch(source->source instanceof net.minecraft.client.resources.ClientPackSource))return;
  Set<RepositorySource> next=new LinkedHashSet<>(sources);next.add(new SoundOverrides());sources=Collections.unmodifiableSet(next);
 }
}
