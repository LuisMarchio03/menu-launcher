# Glitch (o robozinho) — design

Data: 2026-11-03
Projeto: menu-launcher (`docs/superpowers/specs/2026-11-03-meu-launcher-design.md`)
Esboço aprovado: `docs/esboco/robozinho.html`, variante **D · Corpinho completo**

## Intenção

É um bichinho virtual "só para brincar" que mora na tela inicial do launcher. O Brendon queria algo ao mesmo tempo fofo e misterioso, e escolheu um **robozinho de corpo inteiro**. A graça é ele parecer vivo: reage ao que acontece no celular e a você, sem atrapalhar o uso.

## Visual (aprovado no esboço, "depois vamos ajeitando")

- Desenhado só em **contorno**, numa cor só: a **cor do traço** dos Ajustes, igual aos ícones.
- A cabeça é uma caixa arredondada com uma **telinha quadrada** no lugar do rosto, antena e "orelhinhas" laterais. A cabeça é um pouco grande em relação ao corpo.
- O corpo é um retângulo arredondado com dois pontinhos (LEDs) e um risco. Os bracinhos são simples.
- **Pernas de tubo sanfonado**, com anéis, e **pés em cúpula**, no estilo da referência que o Brendon mandou.
- O rosto aparece só dentro da telinha, e nada do rosto pode vazar dela. O "Zzz" do sono é a única exceção: ele sai da cabeça de propósito.
- Altura na tela: cerca de 72 dp.

## Comportamento

### Onde vive
- **Atualização (pedido do Brendon, mesmo dia): anda pela tela inicial inteira**, em qualquer espaço livre, sem pisar no relógio, na música, na lista, na barra de letras nem no símbolo da IA. Quando a tela muda e ele fica em cima de algo, dá um glitch para um lugar livre. O texto abaixo sobre o "chão" foi a primeira versão.
- (primeira versão) **Anda num "chão" invisível** na faixa livre do meio da tela inicial do modo uma mão: abaixo do relógio e do cartão de música, acima da lista de favoritos. Decidido pelo Brendon. Andar pela tela toda fica para depois.
- Anda só para a esquerda e para a direita. O chão vai da margem esquerda até antes da barra de letras e do símbolo da IA, sem passar por cima deles.
- O chão fica numa altura fixa, a uma fração da tela, e não acompanha a lista. A lista de favoritos ocupa no máximo a metade de baixo, então não chega até ele.
- Escolhe um ponto do chão, anda até lá, para um pouco (pisca, olha em volta) e escolhe outro.
- **Não entra nos apps.** Quando um app abre, ele congela onde está e continua dali quando você volta para a tela inicial.
- No **modo grade** ele não aparece, como o símbolo da IA.
- Anda virado para o lado em que está indo.

### Humores pelo estado do celular

Quando mais de um vale ao mesmo tempo, vence o primeiro desta lista:

1. **Bravo** (ver "Interação"): sobrepõe todo o resto enquanto dura.
2. **Dormindo**: das 23h às 7h. Fica parado, com os olhos em tracinho e o "Zzz" subindo.
3. **Cansado**: bateria em 15% ou menos, sem estar carregando. Fica parado, com os olhos caídos e uma bateriazinha na telinha.
4. **Música**: tem música tocando (a mesma informação do cartão de música). Para e dança no lugar, e a telinha vira um **equalizador**, só dentro dela.
5. **Feliz**: por 5 segundos depois que as notificações chegam a zero (havia alguma e agora não há). Mostra um coração na telinha e pula.
6. **Normal**: anda e para, pisca.
   - De vez em quando, numa média de uma vez a cada poucos minutos, ele **escaneia**: uma linha corre na telinha e a antena pisca.
   - Mais raramente, dá um **glitch**: some e reaparece em outro lugar livre.

As notificações e a música usam o acesso às notificações que o launcher já pede. Sem esse acesso, o humor "feliz" e o de música simplesmente nunca acontecem.

### Interação (tudo **só visual, sem som**)
- **Um toque:** dá uma **risadinha**. A telinha faz "^^" e ele dá um pulinho.
- **Importunar:** cada toque aumenta a irritação, e ela vai baixando sozinha com o tempo.
  - Um toque de vez em quando é sempre risadinha.
  - Vários toques em pouco tempo sobem a irritação:
    1. **Sério**: olhos retos, sem risadinha.
    2. **Bravo**: sobrancelhas na telinha, bate o pé e a antena pisca.
    3. **Ameaça** (muito bravo): para, saca uma **AK-47 de brinquedo** e aponta para você, com a arma de frente e o cano para fora da tela, segura no meio do peito pelas duas mãos. **Nunca atira.** Ele não anda com ela: guarda quando se acalma.
    - Depois de guardar a mira, ele **marcha com a arma** atravessada no peito por 20 s, com cara séria e passo duro (pedido do Brendon).
    - **Balão de fala** com frases de raiva em cada nível ("Para com isso!", "Vai encarar?", "Fui!") e frases de marcha ("Esquerda, direita!"). A risadinha não fala.
    4. **Foge**: se você continuar importunando, ele dá um glitch e reaparece longe, **ainda apontando a arma**, até se acalmar.
  - Sem toques, ele se acalma em poucos segundos e volta ao humor normal.
- O toque nele não abre os Ajustes nem passa para o que está por baixo.

### Ajustes
- Ajustes → **Robozinho**: liga e desliga. Já vem **ligado**.

## Não fazer agora (ideias para depois)
- Comer notificações e engordar.
- Esconder-se atrás do relógio e sentar no cartão de música (o "inquilino").
- Crescer e murchar conforme o uso de apps que distraem.
- Virar o corpo da IRIS.
- Arrastá-lo com o dedo.
- Som.
- Variantes de visual (flutuante, rodinha, hélice).

## Anti-objetivos
- Atrapalhar o uso: ficar em cima de algo tocável, roubar toques dos favoritos, das letras ou da IA, ou deixar a tela inicial lenta.
- Gastar bateria quando a tela inicial não está visível: a animação para.
- Sair da cor do traço ou ter cores próprias.

## Decisões minhas
- Ele é desenhado em código (Canvas do Compose), a partir dos caminhos do esboço, para seguir a cor do traço e animar sem imagens.
- A lógica de humor, irritação e escolha de destino é pura (sem Android) e testada na JVM. O desenho e as animações são conferidos por print no aparelho.
- Os horários (23h–7h), o limite de bateria (15%), o tempo do "feliz" (5 s, pedido do Brendon) e o ritmo da irritação são palpites meus, fáceis de ajustar.
- Música: ele dança só quando a música está **tocando de fato** (pausada não conta, mesmo com o cartão ainda na tela). Isso vale mesmo com o cartão de música desligado nos Ajustes.
- Toque durante qualquer humor (dormindo, cansado, dançando): a reação (risadinha, sério ou bravo) aparece por cima por um instante, e depois ele volta ao humor do momento. Tocar não acorda o robô de vez.
- Contagem de notificações: o serviço de notificações que o launcher já tem passa a informar quantas notificações "de verdade" existem, sem contar as persistentes nem as de mídia.

## Autonomia (cérebro do Glitch) — 2026-11-03

Nome escolhido pelo Brendon: **Glitch**. Inspirado nas ideias do projeto Pestinha (GPL-3.0): só as ideias, o código é todo nosso.

- **Vontades** (0–100%): energia, tédio, carência e curiosidade mudam com o tempo e com eventos (música, notificação, toque gentil). Só andam com a tela inicial à vista.
- **Decisão** a cada 7–13 s, pela maior vontade com um pouco de sorte: passear, cochilar, pedir atenção (vem para o meio, acena e fala), explorar (vai até algo da tela e escaneia), ficar emburrado (de costas, resmungando) ou dançar. Com vínculo ruim ele recusa dançar.
- **Vínculo** (salvo entre os dias): sobe com risadinha e desce com raiva, ameaça e fuga. Vínculo alto deixa ele mais paciente; baixo, explosivo.
- **Volta depois de sumir** (mais de 2 h sem ver a tela inicial): ele cumprimenta com saudade ou emburrado, conforme o vínculo.
- **Ajustes → Glitch**: liga e desliga, e mostra ao vivo o que ele está fazendo e as barras das vontades e do vínculo.
- Noite e bateria baixa continuam mandando (dorme ou fica cansado); a raiva, a AK e a marcha passam por cima de tudo.

## Brincadeiras (2026-11-03, ideias inspiradas no Pestinha)

- **Carinho**: passar o dedo indo e voltando em cima dele. Fecha os olhinhos, perdoa tudo (raiva, AK, marcha, protesto, castigo), aumenta o vínculo.
- **Colo e arremesso**: segurar parado pega no colo; soltar arrastando joga, com gravidade, quique e pouso em cima da lista, do cartão da música ou da IA. Jogado forte fica tonto.
- **Cantinho do castigo**: a placa aparece num canto quando você pega ele no colo; soltar ele em cima dela deixa ele 25 s de costas, levantando cartazes ("INJUSTIÇA!", "TÔ DE MAL"). Cutucar muito faz ele fugir; carinho perdoa.
- **Tristeza**: carente e entediado há muito tempo, vai para um canto, cara triste com lágrima e "Ninguém liga pra mim...".
- **Bolinha**: entediado e com energia, solta uma bola do alto e corre atrás chutando; você pode pegar e jogar a bola também ("Ei, devolve a bola!").
- **Protesto**: três raivas em 10 minutos e ele marcha 30 s com um cartaz ("ABAIXO O DEDO!", "GREVE!").
- **Fogos**: quando o vínculo chega a 85%, comemora com fogos e "Melhor humano!".

## Brincadeiras que você começa e mais ideias do Pestinha (2026-11-03)

- **Toque duplo no Glitch** abre o menu "Bora brincar de quê?": Bola, Esconde-esconde, Pega-pega, Jokenpô, Bolhas.
  - **Bola (passe)**: ele chuta de volta para você; você joga a bola com o dedo. Acaba depois de 40 s sem você tocar na bola.
  - **Esconde-esconde**: ele some atrás de algo da tela (só a antena espia); você toca onde acha. Errou: "tá quente" / "tá frio"; 5 erros e ele aparece.
  - **Pega-pega** (25 s): ele foge do seu dedo, encurralado dá glitch; encostou nele, pegou.
  - **Jokenpô**: você escolhe pedra, papel ou tesoura; ele mostra a dele na telinha. Perdeu, fica bravo e pede revanche.
  - **Bolhas** (30 s): cada toque na tela solta uma bolha; ele corre e estoura.
- **Travessura**: entediado e sem carência, cola post-its na tela ("Vai beber água!", "Lembra de mim"); arrastar para longe joga fora (ele reclama).
- **Trabalhar junto**: voltando de um app de trabalho aberto pelo launcher (GitHub, Notas, Obsidian, Office, Hangar...) depois de 2 min, ele aparece com um notebookzinho ("Commitou?").
- **Pião**: girar com dois dedos; solto, continua girando e, se girou muito, fica tonto.
- **Aprender a se encolher**: cutucadas que irritam ensinam ele a esperar cutucada; com a expectativa alta ele se encolhe quando você encosta. Risadinha e carinho desensinam.

## Vida do Glitch (2026-11-03)

- **Acessórios** (todos liberados, sem XP): boné, óculos escuros, fone, capacete, cachecol, gravatinha. Ajustes → Glitch → Acessório.
- **Reações ao celular**: carregando, aparece um cabinho saindo das costas até a base da tela, com faísca de energia, e ele recarrega energia ("Nham, energia!"); carga completa dá fogos. Fone conectado: ele põe fone sozinho. Sem internet: "?" na telinha de vez em quando e "Cadê a internet?".
- **Rotina**: bom dia uma vez por dia entre 6h e 11h; de madrugada (0h–5h) manda você dormir, no máximo a cada 30 min.
- **Diário de hoje** nos Ajustes, na voz dele: quantas vezes você fez carinho, jogou, cutucou, brincou e fez ele fugir.

## Temperamento (2026-11-03, ideias do Pestinha)

Nada aqui bloqueia app: é só reação visual na tela inicial (decisão do Brendon: nenhum modo foco nem disputa para abrir apps).

- **Protesto em estágios**, contando os protestos da última hora: o primeiro é reclamação (30 s marchando com cartaz); o segundo é **greve** (60 s parado, de costas, com cartaz, recusando toque e menu); do terceiro em diante é **revolução** (30 s marchando e atirando bolinhas de papel, só visual). Carinho encerra qualquer um.
- **Rancor**: depois de castigo ou fuga, por 3 min a risadinha vira cara séria com resmungo e não ganha vínculo. Carinho perdoa antes.
- **Medo da placa**: levar ele no colo até a placa do castigo faz ele reclamar uma vez a cada aproximação.
- **Dramático**: com vínculo abaixo de 70%, a raiva às vezes (12%) vira um desmaio fingido, deitado com tontura por 2,8 s.
- **Bilhete triste**: quando fica triste, deixa um post-it perto do canto, um por vez. A tristeza não passa sozinha: dura até a carência baixar com atenção (toque gentil ou carinho).
- **Olhos que seguem**: por 2,5 s depois de um toque em qualquer ponto da tela inicial, os olhos olham para lá. O toque é só observado, nunca consumido.

## Tela de bloqueio (2026-11-04)

Testado no S25 FE (Android 17, One UI 9): uma tela nossa por cima do bloqueio desliga a digital com a tela acesa e vira duas telas de bloqueio. Por isso o Glitch mora no papel de parede do bloqueio: a tela continua sendo a da Samsung, com digital direta, notificações e atalhos, e o papel de parede da tela inicial não muda (a prévia do sistema oferece aplicar só no bloqueio).

- Pequeno e quieto, no canto de baixo, em cima de uma imagem de fundo escolhida nos Ajustes. Não recebe toque.
- O bloqueio fica à vista por poucos segundos: em cerca de 40% das vezes que a tela acende ele faz uma coisa curta (acenar, olhar em volta, escanear, se espreguiçar, pular); depois, só de 20 a 45 s adiante.
- Dorme de noite e no não perturbe, fica cansado com bateria baixa e dança de leve com música.
- Só desenha à vista, e parado redesenha poucas vezes por segundo.

## Sono com gorro (2026-11-04)

Dormindo das 23h às 7h, na tela inicial e no bloqueio, ele usa gorrinho com pompom e tapa-olho, por cima de qualquer acessório escolhido, e o Zzz sobe bem acima da cabeça. No não perturbe de dia, só o tapa-olho. O notebook do trabalho passou a ter tela acesa e teclado, com as mãos nas laterais.

## Vida no aparelho (2026-11-04)

- **Sente o celular**: três trancos em 1 s deixam tonto; inclinado mais de cerca de 40°, escorrega para o lado de baixo; de cabeça para baixo, reclama e fica em pé em relação ao mundo. Acelerômetro só com a tela inicial à vista.
- **Comenta**: ao voltar de um app, no máximo a cada 10 min e em 40% das vezes. Pode desligar.
- **Travessuras**: entorta o relógio ou empurra o primeiro favorito por até 3 min; cutucar ele desfaz. Pode desligar.
- **Print**: faz pose ao detectar captura da tela inicial.
- **Datas**: Natal (20 a 25/12), Ano-Novo, Halloween e aniversário (campo nos Ajustes), com chapéu e fala do bom-dia.
- **Comer notificações**: vai até a bolinha e come; a barriga enche e esvazia em cerca de 3 min. A notificação não é tocada, e a bolinha volta em 1 min.
- **Poleiro**: senta em cima da lista ou se pendura embaixo do relógio, lugares sem nada tocável.
- **Caça aos bugs** (25 s) e **Duelo** (5 rodadas de reflexo, com recorde salvo) no menu de brincadeiras.
- **Clima**: só com a cidade escrita nos Ajustes (Open-Meteo, sem chave). Guarda-chuva, óculos ou cachecol, e o acessório escolhido vence.
- **Falar com a IA**: pelo menu do Glitch; ele mostra "Ouvindo..." e "Pensando...", e a resposta volta no balão.
- **Bloqueio**: cabinho carregando e cartinha quando chega notificação.

