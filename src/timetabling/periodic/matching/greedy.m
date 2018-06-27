function [ giveback ] = greedy( Nodes,Matchingedges )
%wir wollen Zwei Knoten zur�ckgeben, welche dann vereinigt werden
%Matchingedges gibt unsere Kantengewichte bzgl der Linien an, dies sind
%aber nicht die Gewichte bez�glich der Knoten, diese werden in Nodeweights
%gesammelt
Nodeweights=sparse(size(Nodes,2),size(Nodes,2));
%Nodes{2}=[Nodes{2} Nodes{4}]
[LineA,LineB,Umsteiger]=find(Matchingedges);


%dazu ist es n�tig zu bestimmen welchen Knoten die zZ betrachteten 2 Linien
%angeh�ren um dann den Eintrag in Nodeweights zu setzen
for i=1:size(LineA,1)

        [a,b,bob]=cellfun(@(tim) find(tim==LineA(i)), Nodes, 'UniformOutput', false);
        LineANode=find(cellfun(@isempty,bob)==0); %Wir haben unseren ersten Matrixindex

        [a,b,bob]=cellfun(@(tim) find(tim==LineB(i)), Nodes, 'UniformOutput', false);
        LineBNode=find(cellfun(@isempty,bob)==0); %Wir haben unseren zweiten Matrixindex
        %und k�nnen nun unseren Eintrag vornehmen:
        Nodeweights(LineANode,LineBNode)=Nodeweights(LineANode,LineBNode)+Umsteiger(i);

end

Nodeweights=triu(triu(Nodeweights)+triu(Nodeweights'),1);%von A nach B oder B nach A ist wurscht, aber A nach A brauchen wir nicht

%Nun noch die Knoten mit maximaler Verbindung raussuchen
[row,col]=find(Nodeweights==max(Nodeweights(:)),1);
%falls wir auf den Kantenknoten nur noch 0 stehen haben, also nichts mehr
%verschmelzen k�nnen, geben wir besser NaN zur�ck!
if(max(Nodeweights(:))<=0)
    row=NaN;
    col=NaN;
end

giveback=[row, col];
return;

end

