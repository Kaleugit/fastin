# ---------------------------------------------------------------- Room
# O Room gera implementações e as resolve por nome; manter DAOs e database.
-keep class dev.kaleu.fastin.data.db.** { *; }

# ---------------------------------------------------------------- kotlinx.serialization
# Regras oficiais. A config do dashboard é desserializada em runtime a partir do DataStore:
# se o R8 remover um serializer, o usuário perde os cards que configurou — e só descobre
# depois de instalar. Barato demais para arriscar.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Enums serializados pelo nome (ChartType, Metric, Period, Aggregation): renomear quebraria
# a config já gravada no aparelho.
-keepclassmembers enum dev.kaleu.fastin.domain.metrics.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------------------------------------------------------------- WorkManager
# O worker das notificações é instanciado por reflexão a partir do nome no manifesto.
-keep class dev.kaleu.fastin.notify.MilestoneWorker { <init>(...); }
