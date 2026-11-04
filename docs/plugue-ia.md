# Plugue de IA do menu-launcher

Este é o contrato para uma IA (IRIS, Eloy ou qualquer outra) aparecer na tela inicial do menu-launcher. Não é preciso ler o código do launcher.

O que o usuário vê:

- **Primeira abertura** (ou Ajustes → IA): o launcher lista as IAs instaladas que seguem este contrato e conecta a escolhida.
- **Símbolo flutuante**: o símbolo da IA fica na tela inicial, e dá para arrastá-lo para qualquer lugar.
- **Tocar** no símbolo abre a IA.
- **Segurar** no símbolo: o usuário fala o comando e solta. O launcher transcreve a fala no próprio aparelho (pt-BR) e entrega **o texto** para a IA, sem abrir o app dela.

A IA não lida com áudio, só com texto. Se ela não implementar a parte de comandos, segurar faz o mesmo que tocar.

**3a ou 3b?** O Android 15+ não deixa app em segundo plano abrir tela. Se a IA só responde (fala, notifica, anota), use **3a**: ela recebe o texto sem aparecer. Se os comandos dela abrem outros apps ("abre o YouTube", "toca X no Spotify"), use **3b**: o launcher, que está em primeiro plano, abre a IA com o texto, e a partir daí ela pode abrir o que quiser. Com as duas declaradas, o launcher usa a 3a. A IRIS usa a 3b.

## As três peças

Todas são declaradas no `AndroidManifest.xml` da IA.

| Peça | Obrigatória? | O que declarar |
|---|---|---|
| 1. Descoberta e toque | sim | uma **Activity** exportada com `<action android:name="com.brendon978.menulauncher.action.ASSISTANT"/>` e `<category android:name="android.intent.category.DEFAULT"/>` |
| 2. Símbolo | recomendada | na mesma Activity, um `<meta-data android:name="com.brendon978.menulauncher.symbol" android:resource="@drawable/..."/>` |
| 3a. Comandos sem abrir | opcional | um **BroadcastReceiver** exportado com `<action android:name="com.brendon978.menulauncher.action.COMMAND"/>`. O texto chega no extra `android.intent.extra.TEXT` (`Intent.EXTRA_TEXT`) |
| 3b. Comandos abrindo a IA | opcional | uma **Activity** exportada com a mesma action `COMMAND` + categoria `DEFAULT`; o launcher abre a IA já com o texto em `EXTRA_TEXT` |

Detalhes:

- **Símbolo:** de preferência um vetor de contorno com 24×24 de viewport. O launcher pinta o símbolo inteiro numa cor só, a cor do traço que o usuário escolheu, para combinar com os ícones. Por isso, só o formato importa, não as cores. Sem símbolo, o launcher usa o ícone de contorno do app.
- **Toque:** o launcher abre a Activity da peça 1 com a action `ASSISTANT`. A IA pode usar isso para, por exemplo, já abrir no modo de escuta.
- **Comando:** chega como broadcast **explícito**, direto para o componente do receiver, então não sofre o limite de broadcasts implícitos. O `onReceive` tem poucos segundos para rodar. Para trabalho longo (chamar LLM, falar), inicie um serviço em primeiro plano ou use `goAsync()`. O launcher não espera resposta: a IA responde do jeito dela, falando, notificando ou agindo.
- **Abrir tela a partir do receiver:** o Android bloqueia apps em segundo plano de abrir Activity. Se a IA precisar mostrar algo, use uma notificação.

## Falar pelo Glitch (opcional)

A IA também pode falar pelo Glitch, o robozinho da tela inicial. Basta mandar um broadcast para o launcher:

- `action`: `com.brendon978.menulauncher.action.SAY`
- pacote: `com.brendon978.menulauncher` (use `setPackage`)
- `android.intent.extra.TEXT`: o que falar (o balão corta em 140 caracteres; marcações como `[OK]` e links são tirados)
- `com.brendon978.menulauncher.extra.FROM`: quem fala, por exemplo `IRIS` (aparece antes do texto)

Se a tela inicial não estiver aberta, a fala fica guardada e sai quando o usuário voltar (até 10 minutos depois). Só a última fica guardada.

```kotlin
context.sendBroadcast(
    Intent("com.brendon978.menulauncher.action.SAY")
        .setPackage("com.brendon978.menulauncher")
        .putExtra(Intent.EXTRA_TEXT, "Lembrete: dentista às 16h")
        .putExtra("com.brendon978.menulauncher.extra.FROM", "IRIS"),
)
```

## Android nativo (Kotlin)

O módulo `ia-exemplo/` deste repositório é um app completo e mínimo com as três peças. Veja `ia-exemplo/src/main/AndroidManifest.xml` e `CommandReceiver.kt`.

```xml
<activity android:name=".MainActivity" android:exported="true">
    <intent-filter>
        <action android:name="com.brendon978.menulauncher.action.ASSISTANT" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
    <meta-data android:name="com.brendon978.menulauncher.symbol" android:resource="@drawable/simbolo" />
</activity>

<receiver android:name=".CommandReceiver" android:exported="true">
    <intent-filter>
        <action android:name="com.brendon978.menulauncher.action.COMMAND" />
    </intent-filter>
</receiver>
```

```kotlin
class CommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val texto = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        // entregue para o cérebro da IA
    }
}
```

## .NET MAUI (o caso da IRIS)

No MAUI, os intent-filters saem dos atributos C#. Esta é uma sugestão para `Platforms/Android/`; quem decide a implementação é o repositório da IA.

```csharp
// Na MainActivity existente: IntentFilters do toque (ASSISTANT) e do comando abrindo a IA (3b),
// mais o MetaData do símbolo. O texto chega em OnCreate/OnNewIntent: Intent.GetStringExtra(Intent.ExtraText).
[IntentFilter(new[] { "com.brendon978.menulauncher.action.ASSISTANT" },
    Categories = new[] { Android.Content.Intent.CategoryDefault })]
[IntentFilter(new[] { "com.brendon978.menulauncher.action.COMMAND" },
    Categories = new[] { Android.Content.Intent.CategoryDefault })]
[MetaData("com.brendon978.menulauncher.symbol", Resource = "@drawable/simbolo_iris")]
public class MainActivity : MauiAppCompatActivity { /* ... */ }

// Alternativa 3a (sem abrir a IA), para quem só responde: ReceptorComandoLauncher.cs
[BroadcastReceiver(Exported = true)]
[IntentFilter(new[] { "com.brendon978.menulauncher.action.COMMAND" })]
public class ReceptorComandoLauncher : BroadcastReceiver
{
    public override void OnReceive(Context? context, Intent? intent)
    {
        var texto = intent?.GetStringExtra(Intent.ExtraText);
        if (string.IsNullOrWhiteSpace(texto)) return;
        // Pending result + Task.Run para trabalho longo, ou um serviço em primeiro plano.
        // Entregue `texto` para o CortexEngine como se fosse digitado.
    }
}
```

`@drawable/simbolo_iris` é um vetor em `Platforms/Android/Resources/drawable/` com o desenho da íris em contorno.

## Testar

1. Instale o launcher e a IA no celular.
2. No launcher, segure numa área vazia → Ajustes → IA. A IA deve aparecer na lista. Se não aparecer, a peça 1 está faltando ou a Activity não está exportada.
3. Toque no símbolo: a IA abre.
4. Segure o símbolo, fale e solte. Aparece o toast "<IA>: <texto>" e o receiver da IA recebe o texto. Na primeira vez, o launcher pede o microfone.

Teste por ADB, sem o launcher:

```sh
adb shell am broadcast -a com.brendon978.menulauncher.action.COMMAND \
  -n <pacote>/<receiver> --es android.intent.extra.TEXT "que horas são"
```

## Estabilidade

Os nomes `com.brendon978.menulauncher.action.ASSISTANT`, `com.brendon978.menulauncher.action.COMMAND` e `com.brendon978.menulauncher.symbol` são o contrato. Mudar qualquer um deles quebra a IRIS e a Eloy, então só muda com aviso aos dois. Uma ideia para o futuro, ainda fora do contrato: a IA informar o próprio estado (pronta, ouvindo, pensando) para o símbolo animar.

## Resposta do receiver de comandos

O launcher entrega o comando por broadcast ordenado. Quem resolve sem abrir nada responde `setResultCode(Activity.RESULT_OK)`. Quem não responder, e tiver a Activity de comando (forma 3b), é aberto por ela. Assim a IA pode trabalhar em silêncio quando já está rodando, e abrir só quando estiver fechada.

