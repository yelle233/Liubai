package com.yelle233.liubai.client;

import com.yelle233.liubai.visibility.VisibilityService;

/** One-second rates plus current gauges; avoids render-frame/tick aliasing in the HUD. */
public final class RenderStatistics {
    private int entitiesTested, entitiesSkipped, entitiesOccluded, entitiesTooSmall, entitiesDensityLimited;
    private int blockEntitiesTested, blockEntitiesSkipped, blockEntitiesOccluded;
    private int shadowsSkipped, nameTagsSkipped, particlesAccepted, particlesSkipped, temporalUpdatesDeferred, flywheelLimiterAdjusted;
    private int valkyrienEntitiesBypassed, valkyrienBlockEntitiesBypassed, valkyrienParticlesBypassed, valkyrienFlywheelBypassed;
    private long[] window = new long[18];
    private long windowStartNanos = System.nanoTime();
    private Rate rate = Rate.EMPTY;
    private VisibilityService.Counts visibility = new VisibilityService.Counts(0, 0, 0, 0, 0, 0, 0);
    private int liveParticles;

    public void entity(RenderDecision decision) { entitiesTested++; if (decision.skip()) { entitiesSkipped++; if (decision.reason()==RenderDecision.Reason.OCCLUDED) entitiesOccluded++; if (decision.reason()==RenderDecision.Reason.TOO_SMALL) entitiesTooSmall++; if (decision.reason()==RenderDecision.Reason.DENSITY_LIMIT) entitiesDensityLimited++; } }
    public void blockEntity(RenderDecision decision) { blockEntitiesTested++; if (decision.skip()) { blockEntitiesSkipped++; if (decision.reason()==RenderDecision.Reason.OCCLUDED) blockEntitiesOccluded++; } }
    public void shadowSkipped(){shadowsSkipped++;} public void nameTagSkipped(){nameTagsSkipped++;}
    public void particleAccepted(){particlesAccepted++;} public void particleSkipped(){particlesSkipped++;}
    public void temporalUpdateDeferred(){temporalUpdatesDeferred++;}
    public void flywheelLimiterAdjusted(){flywheelLimiterAdjusted++;}
    public void valkyrienEntityBypassed(){valkyrienEntitiesBypassed++;} public void valkyrienBlockEntityBypassed(){valkyrienBlockEntitiesBypassed++;} public void valkyrienParticleBypassed(){valkyrienParticlesBypassed++;} public void valkyrienFlywheelBypassed(){valkyrienFlywheelBypassed++;}

    public void finishFrame(VisibilityService.Counts visibility, int liveParticles) {
        this.visibility=visibility; this.liveParticles=liveParticles;
        int[] values={entitiesTested,entitiesSkipped,entitiesOccluded,entitiesTooSmall,entitiesDensityLimited,blockEntitiesTested,blockEntitiesSkipped,blockEntitiesOccluded,shadowsSkipped,nameTagsSkipped,particlesAccepted,particlesSkipped,temporalUpdatesDeferred,flywheelLimiterAdjusted,valkyrienEntitiesBypassed,valkyrienBlockEntitiesBypassed,valkyrienParticlesBypassed,valkyrienFlywheelBypassed};
        for(int i=0;i<values.length;i++)window[i]+=values[i]; long now=System.nanoTime(),elapsed=now-windowStartNanos;
        if(elapsed>=1_000_000_000L){double seconds=elapsed/1_000_000_000.0;int[] perSecond=new int[window.length];for(int i=0;i<window.length;i++)perSecond[i]=(int)Math.round(window[i]/seconds);rate=new Rate(perSecond);window=new long[18];windowStartNanos=now;}
        entitiesTested=entitiesSkipped=entitiesOccluded=entitiesTooSmall=entitiesDensityLimited=0;blockEntitiesTested=blockEntitiesSkipped=blockEntitiesOccluded=0;shadowsSkipped=nameTagsSkipped=particlesAccepted=particlesSkipped=temporalUpdatesDeferred=flywheelLimiterAdjusted=0;valkyrienEntitiesBypassed=valkyrienBlockEntitiesBypassed=valkyrienParticlesBypassed=valkyrienFlywheelBypassed=0;
    }
    public Snapshot snapshot(){return new Snapshot(rate.entitiesTested,rate.entitiesSkipped,rate.entitiesOccluded,rate.entitiesTooSmall,rate.entitiesDensityLimited,rate.blockEntitiesTested,rate.blockEntitiesSkipped,rate.blockEntitiesOccluded,rate.shadowsSkipped,rate.nameTagsSkipped,rate.particlesAccepted,rate.particlesSkipped,rate.temporalUpdatesDeferred,rate.flywheelLimiterAdjusted,rate.valkyrienEntitiesBypassed,rate.valkyrienBlockEntitiesBypassed,rate.valkyrienParticlesBypassed,rate.valkyrienFlywheelBypassed,visibility.visible(),visibility.occluded(),visibility.unknown(),visibility.queued(),visibility.checked(),visibility.timedOut(),visibility.micros(),liveParticles);}
    private record Rate(int entitiesTested,int entitiesSkipped,int entitiesOccluded,int entitiesTooSmall,int entitiesDensityLimited,int blockEntitiesTested,int blockEntitiesSkipped,int blockEntitiesOccluded,int shadowsSkipped,int nameTagsSkipped,int particlesAccepted,int particlesSkipped,int temporalUpdatesDeferred,int flywheelLimiterAdjusted,int valkyrienEntitiesBypassed,int valkyrienBlockEntitiesBypassed,int valkyrienParticlesBypassed,int valkyrienFlywheelBypassed){private static final Rate EMPTY=new Rate(new int[18]);private Rate(int[]v){this(v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],v[8],v[9],v[10],v[11],v[12],v[13],v[14],v[15],v[16],v[17]);}}
    public record Snapshot(int entitiesTested,int entitiesSkipped,int entitiesOccluded,int entitiesTooSmall,int entitiesDensityLimited,int blockEntitiesTested,int blockEntitiesSkipped,int blockEntitiesOccluded,int shadowsSkipped,int nameTagsSkipped,int particlesAccepted,int particlesSkipped,int temporalUpdatesDeferred,int flywheelLimiterAdjusted,int valkyrienEntitiesBypassed,int valkyrienBlockEntitiesBypassed,int valkyrienParticlesBypassed,int valkyrienFlywheelBypassed,int visible,int occluded,int unknown,int queued,int visibilityChecks,int visibilityTimeouts,long visibilityMicros,int liveParticles){}
}
