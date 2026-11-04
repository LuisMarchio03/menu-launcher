# menu-launcher — design

Data: 2026-11-03
Repositório: `git@github-pessoal:Brendon978/menu-launcher.git` (conta pessoal; a pasta local é `~/brendon projects/meu-launcher`)

## Intenção e o porquê

O Brendon quer **estilizar o próprio celular** do jeito dele, com autonomia de verdade sobre a tela inicial, em vez de depender do launcher da Samsung (One UI) ou de launchers da Play Store. O app é **só dele e de um amigo**: não vai para a Play Store, é instalado direto nos aparelhos por ADB.

Ele usa o celular **quase sempre com uma mão só**. A referência que ele trouxe é o Niagara Launcher (`bitpit.launcher`): lista de apps na metade de baixo e barra de letras curva na lateral, alcançável pelo polegar. Ele gosta de **ícones minimalistas, só contorno, de uma cor** e não gosta de ícones coloridos.

Ainda não tem um visual final decidido. O valor do app está em **poder mudar a tela inicial com o tempo**, não em chegar a um visual fixo. A expectativa é ir atualizando o app aos poucos.

Os dois usuários estão desenvolvendo a própria IA: o Brendon tem a **IRIS** (`~/brendon projects/Cortex_Iris`, app Android em .NET MAUI, com comandos por texto e voz) e o amigo tem a **Eloy**. Eles querem que a IA de cada um tenha um lugar na tela inicial.

## Aparelhos-alvo

- Brendon: Samsung Galaxy S25 FE (SM-S731B), Android 17 (API 37), One UI 9, tela de 1080×2340 com densidade 450.
- Amigo: Samsung Galaxy S24, atualizado (mesma geração de Android e One UI). A tela é menor, então o layout tem que se ajustar ao tamanho da tela e não pode depender de medidas fixas em pixel.
- Não precisa suportar outros fabricantes, tablets ou versões antigas do Android.
- Instalação por ADB pela rede. O S25 FE já está pareado pelo Tailscale (`100.95.14.17`).

## Objetivos

1. **Tela inicial própria**: o app é registrado como launcher (HOME) e o usuário o define como padrão no lugar do One UI Home.
2. **Dois modos**, trocáveis pelo usuário:
   - **Uma mão** (o modo principal): relógio, data e a faixa que está tocando no topo, só para olhar. Na metade de baixo, a lista de **favoritos** (ícone + nome) e os **controles de música** (⏮ ⏯ ⏭), ao alcance do polegar. Na borda direita, uma **barra de letras curva** (★ e A–Z): tocar ou arrastar numa letra mostra os apps dela no lugar dos favoritos, e ★ volta aos favoritos.
   - **Padrão**: grade com **todos os apps** em ordem alfabética, ícones de contorno com o nome embaixo. Ainda não é uma tela montada à mão (páginas, arrastar para posicionar). Essa evolução pode vir depois.
   - O esboço aprovado está em `docs/esboco/tela-inicial.html` (abre no navegador e é interativo). O Brendon disse que ficou "perfeito" e ele vale como referência visual. Pontos do esboço: fundo escuro, relógio grande e fino, cartão de música com contorno sutil, ícones de traço de cerca de 1,6 px em 24 dp, barra de letras com curva para dentro no meio. **Decidido depois do esboço (2026-11-03, testando no aparelho):** no modo uma mão, os controles de música ficam logo abaixo do cartão, no topo — o Brendon achou mais bonito que no fim da tela. No modo grade, o player (cartão + controles) fica ao lado do relógio.
3. **Ícones de contorno para todos os apps**, inclusive os que forem instalados depois, sem os logos originais. Exemplo dado por ele: o Spotify vira só o círculo com os três riscos no centro, em contorno.
   - Estilo único para todos os apps. **A cor do traço não muda com o papel de parede.** Ela é fixa, escolhida uma vez nos ajustes e só muda se o usuário quiser.
   - Fontes do ícone, em ordem: (a) o pacote **Lawnicons** (ícones de contorno, código aberto, milhares de apps); (b) se o app não estiver no Lawnicons, a camada monocromática do ícone adaptativo (a mesma usada nos ícones temáticos do Android), convertida em contorno; (c) se não houver nenhuma das duas, a inicial do app dentro de um círculo de contorno.
   - O usuário pode **trocar manualmente o ícone de qualquer app** (escolhendo outro ícone do pacote).
4. **Papel de parede a partir de arquivos que o usuário baixa** (Pinterest, hdqwalls e outros sites). O app não cria papéis de parede, só aplica os que o usuário escolhe:
   - **Estático** (JPG, PNG, WEBP), aplicado na tela inicial, na de bloqueio ou nas duas.
   - **Animado** (MP4 e GIF), como papel de parede animado do Android, **só na tela inicial**, parado com a tela apagada para não gastar bateria. Se, ao aplicar, o One UI também mostrar o animado na tela de bloqueio, tudo bem, mas isso não é requisito.
5. **Espaço para a IA do usuário** (ver a seção "Plugue de IA"):
   - Na primeira abertura, o launcher pergunta se o usuário tem uma IA para conectar e lista as IAs instaladas compatíveis. Conectar é opcional e pode ser feito ou desfeito depois nos ajustes.
   - Com uma IA conectada, o **símbolo dela flutua na tela inicial**, na zona do polegar, e o usuário pode arrastá-lo para onde quiser.
   - **Tocar** no símbolo abre o app da IA.
   - **Segurar** no símbolo abre a escuta: o usuário fala o comando e solta, e o launcher entrega o comando à IA sem abrir o app dela. Se a IA não aceitar comandos, segurar faz o mesmo que tocar.
6. **Ajustes dentro do próprio app, no celular**: modo, favoritos e ordem, cor do traço, troca de ícone por app, papel de parede, mostrar ou esconder o cartão de música, IA conectada. Não é preciso o computador para mudar nada disso.

## Plugue de IA (contrato entre o launcher e as IAs)

É o único contrato com outros apps. A IRIS (Brendon) e a Eloy (amigo) vão implementá-lo, cada uma no próprio repositório, então ele precisa ser simples, estável e documentado num arquivo próprio do repositório (`docs/plugue-ia.md`), para o amigo seguir sem ler o código do launcher.

Desenho (minha decisão; quem constrói pode ajustar os nomes, mas não a forma):

- **Descoberta:** a IA declara no manifesto uma Activity exportada com o intent-filter `action = com.brendon978.menulauncher.action.ASSISTANT`. O launcher lista os apps que respondem a essa ação, mais a opção "outro app", para conectar qualquer app instalado só com o toque de abrir.
- **Símbolo:** a IA indica, num `meta-data` dessa Activity, um drawable (de preferência vetor) para o símbolo flutuante. Sem ele, o launcher usa o ícone de contorno do app (a mesma regra do objetivo 3).
- **Abrir (toque):** o launcher inicia essa Activity, ou a Activity principal do app quando for "outro app".
- **Comando (segurar):** o launcher transcreve a fala com o reconhecimento de voz do Android, no próprio aparelho, e envia o texto à IA por um broadcast explícito (para um BroadcastReceiver da IA, assim o app dela não abre) com `action = com.brendon978.menulauncher.action.COMMAND` e o extra de texto `android.intent.extra.TEXT`. A IA declara se aceita comandos com um receiver com essa ação. Contrato completo em `docs/plugue-ia.md`; app de exemplo em `ia-exemplo/`. Quem recebe pode responder do jeito dela (falar, notificar, agir) e não devolve nada ao launcher.
- Ideia para depois, fora desta versão: a IA informar o próprio estado (pronta, ouvindo, pensando, fora do ar) para o símbolo flutuante animar, como a íris já faz dentro do app IRIS.
- Mudanças na IRIS para implementar o plugue ficam fora deste projeto. São trabalho do repositório Cortex_Iris.

## Fora do escopo (por enquanto)

- Mudar a tela de bloqueio além do papel de parede estático. O Android não deixa um app trocar a tela de bloqueio de verdade. Relógio, atalhos e o resto dela, no Samsung, são com o Good Lock (LockStar).
- Papel de parede animado na tela de bloqueio como requisito.
- Outros modos além de uma mão e padrão. O código deve deixar espaço para modos novos, mas não cria nenhum agora.
- Modo padrão montado à mão (páginas, arrastar para posicionar, gaveta separada).
- Criar ou gerar papéis de parede.
- Estado da IA no símbolo flutuante.
- Publicar na Play Store, outros fabricantes, conta e sincronização entre os aparelhos.

## Anti-objetivos (o que seria fracasso mesmo funcionando)

- Ícones coloridos ou logos originais aparecendo na tela, a não ser como último recurso explícito, e mesmo aí em contorno (a inicial no círculo).
- Algo do uso diário (favoritos, letras, controles de música, símbolo da IA) fora do alcance do polegar no modo uma mão.
- Visual que muda sozinho conforme o papel de parede (ele pediu estilo fixo).
- Precisar do computador para personalizar.
- Exigir uma IA para usar o launcher. Sem IA conectada, não sobra nenhum espaço vazio nem lembrete insistente.

## Restrições

- App Android nativo em Kotlin (minha decisão, ver abaixo).
- Instalação por ADB, sem Play Store.
- Ambiente desta máquina: Android SDK em `~/android-sdk`, com a plataforma `android-36` e build-tools 36.0.0 (o S25 FE é API 37); JDK 26 instalado; sem Gradle nem Kotlin globais (usar o Gradle wrapper do projeto e baixar o que faltar do SDK).
- Git e GitHub só pela conta pessoal (Brendon978), pelo host SSH `github-pessoal`. Nunca usar a conta de trabalho neste repositório.

## Decisões minhas (o Brendon não escolheu, ficaram por minha conta)

- **Kotlin + Jetpack Compose**, porque é o caminho nativo e deixa a interface fácil de mudar depois.
- **minSdk 35** (Android 15), já que os dois aparelhos estão atualizados. O targetSdk acompanha o SDK instalado.
- Nome do pacote: `com.brendon978.menulauncher`.
- **Lawnicons** como base dos ícones. A licença precisa ser conferida antes de incluir os arquivos no repositório.
- **Ajustes acessíveis por toque longo numa área vazia da tela inicial.** Toque longo num app abre opções: favoritar, trocar ícone, informações do app, desinstalar.
- O cartão de música e os controles usam a sessão de mídia do Android, o que exige a permissão de acesso a notificações, pedida na primeira abertura junto com a pergunta da IA. Sem música tocando, o cartão e os controles somem.
- No plugue de IA, o launcher transcreve a fala e manda texto, para a IA não precisar lidar com áudio. O desenho do plugue acima também é meu.
- Os favoritos começam com sugestões óbvias (Telefone, Mensagens, Câmera), e o usuário organiza depois.
- A primeira abertura também pede para definir o app como launcher padrão.

## O que fica por conta de quem for construir

- Arquitetura interna, armazenamento dos ajustes (DataStore, por exemplo) e estrutura de pacotes.
- O algoritmo que converte a camada monocromática em contorno, desde que o resultado combine visualmente com o traço do Lawnicons.
- Como a lista de apps se atualiza ao instalar e desinstalar (por exemplo, `LauncherApps` e os callbacks dele).
- O mecanismo do papel de parede animado (serviço de papel de parede com decodificação de vídeo e GIF).
- Gestos e animações, desde que fiquem leves e fiéis ao esboço.
- Um app de teste mínimo, ou um modo de depuração, que implemente o plugue de IA para validar o contrato antes de a IRIS implementá-lo.

## Depois da primeira versão (2026-11-03)

- **Busca**: lupa na curva da barra (uma mão) e barra fixa embaixo (grade); deslizar para cima na tela inicial também abre.
- **Deslizar para baixo** na tela inicial abre a cortina de notificações.
- **Renomear e esconder apps** pelo toque longo no app. Escondidos somem da lista e da grade, mas a busca ainda acha; voltam em Ajustes → Apps escondidos.
- **Backup**: Ajustes → Backup exporta e importa um arquivo de texto com os ajustes e a memória do Glitch.
- **Contorno de progresso** na caixa da música.
- **Ícones próprios** (assets/proprios) para os apps que o Lawnicons não tem: IRIS, Glitch (este launcher), Build Garage, Apenas Mais Um Nerd, Patronus, barbearia, Sicredi, SouGov, HDQWalls, jogo da Netflix, Promedico, Tofu Drifter, Hangar. Entram depois do Lawnicons e antes do monocromático.
- **Pastas** no modo grade: segurar um app → Colocar em pasta (nova ou existente); a pasta mostra até 4 mini ícones e abre numa folha; "Tirar da pasta" desfaz; pasta vazia some.
- **Dois toques no fundo apagam a tela**: opcional (Ajustes → Sistema), desligado por padrão; usa um serviço de acessibilidade que só apaga a tela e não lê nada. Se o serviço não estiver ligado, o toque duplo abre as configurações de acessibilidade.
