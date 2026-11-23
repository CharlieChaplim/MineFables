# Impetuo v10 — fix definitivo de ancoragem de primeira pessoa

Corrige o erro de origem: RenderHandEvent ocorre antes do Minecraft aplicar a transformacao visual do braco. Em vez de offsets de camera, agora desenhamos o aro usando o ModelPart do braco renderizado por PlayerRenderer.

- Primeira pessoa com ambas as maos vazias: anel ao redor do punho, usando transform do braco vanilla.
- Primeira pessoa com item em qualquer mao: anel oculto.
- Terceira pessoa: sem mudancas.
- Dano, cooldown, Estamina, sincronizacao e particulas: sem mudancas.

Arquivos modificados:
- MineFablesClient.java: remove listener obsoleto de RenderHandEvent.
- ImpetuoFirstPerson.java: remove posicionamento em camera e passa a seguir ModelPart.
- ImpetuoArmFirstPersonMixin.java: chama renderizador ao terminar de renderizar a mao vanilla.
- minefables.mixins.json: registra o novo mixin de cliente.

Requer as atualizacoes anteriores do Impetuo e a infraestrutura mixin do Dragon Install.

Instalar: extraia na raiz do MineFables e execute .\gradlew.bat clean compileJava seguido de .\gradlew.bat runClient.

Limites: nao foi compilado nem testado dentro do Minecraft neste ambiente. O local de ancoragem foi escolhido a partir das dimensoes vanilla da mao; se houver incompatibilidade com outro mod que substitui a renderizacao do braco, pode ser preciso adaptar o mixin.
