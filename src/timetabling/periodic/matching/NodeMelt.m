function [ Nodes ] = NodeMelt( Nodes, combine )
%Nun m�ssen wir noch Knoten innerhalb des Matchinggraphen M verschmelzen:


for i=1:size(combine,1)
    Nodes{combine(i,1)}=[Nodes{combine(i,1)} ;Nodes{combine(i,2)}];
    %Alten rausl�schen
    Nodes{combine(i,2)}=[];%also alten Knoten leeren
end

index = ~cellfun(@isempty, Nodes);%und nur noch nicht leere indices sammeln
Nodes=Nodes(index);

return


end

