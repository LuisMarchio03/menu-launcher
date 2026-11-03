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
5. **Feliz**: por 1 minuto depois que as notificações chegam a zero (havia alguma e agora não há). Mostra um coração na telinha e pula.
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
- Os horários (23h–7h), o limite de bateria (15%), o tempo do "feliz" (1 min) e o ritmo da irritação são palpites meus, fáceis de ajustar.
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
