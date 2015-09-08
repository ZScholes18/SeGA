/**
 * Created by Administrator on 2015/8/26.
 */
(function(){
    var configApp = angular.module('processConfig',[]);
  configApp.controller("serviceController",function($scope){

  })
    var graph = new joint.dia.Graph;
    var current_selected='';//current selected cell
    var paper = new joint.dia.Paper({
        el:$('#draw'),
        width:"100%",
        height:600,
        model:graph,
        gridSize:1,
//    	elementView: glpElementView,
        defaultLink: new joint.shapes.sega.Link({
            attrs:{
                '.connection-wrap':{display:"none"},
                '.link-tools':{display:'none'},
            }
        }),
        linkView: joint.shapes.sega.LinkView,
        interactive: false,
        validateConnection: function(cellViewS, magnetS, cellViewT, magnetT, end, linkView) {
            // Prevent loop linking

            return (magnetS !== magnetT);
        }
    }).on("cell:pointerclick",function(evt,x,y){
            if(current_selected!=""){
                current_selected.unhighlight();

            }
            if(evt.model.prop("type")=="sega.Task"){
                current_selected=evt;
                current_selected.highlight();
                $('#config-modal').modal();
                $('#config-modal #myModalLabel').html(evt.model.attr(".label/text"))
            }
        });
        paper.fitToContent({
            minWidth:document.body.clientWidth ,
            minHeight:600,

        });

    graph.fromJSON(process_json);

    $("button#btn_viewjson,button#btn-navbar-submit").on("click",function(e){
		var json = JSON.stringify(graph.toJSON());
		var svg = paper.svg;
		$("#modal_viewjson .modal-body p").html(json);
		$("#modal_viewjson input#processJSON").val(json);
		$("#modal_viewjson input#svg").val(svg);
		$("#modal_viewjson").modal();
	});


    $('#RW-tab').jstree({
        "core" : {
            "animation" : 200,
            "check_callback" : true,
            "themes" : {
                "stripes" : true,
                "dots" : false
            },
            'data' : entity_json
        },
        "types" : {
            "#" : {
                "max_children" : 1,
                "valid_children" : [ "artifact" ]
            },
            "artifact" : {
                "icon" : "images/tree_icons/artifact.svg",
                "valid_children" : [ "artifact", "attribute", "artifact_n","group","key" ]
            },
            "artifact_n" : {
                "icon" : "images/tree_icons/artifact_n.svg",
                "valid_children" : [ "artifact", "attribute", "artifact_n","group","key" ]
            },
            "attribute" : {
                "icon": "images/tree_icons/attribute.svg",
                "valid_children" : []
            },
            "group" : {
                "icon" : "images/tree_icons/group.svg",
                "valid_children" : ["attribute"]
            },
            "key" : {
                "icon": "images/tree_icons/key.svg",
                "valid_children" : []
            }
        },
        "plugins" : [  "dnd",  "types", "wholerow", "sega" ]

    });

    tree = $("#RW-tab").jstree(true);

    $("#RW-tab").on("ready.jstree" ,function(){
        tree.open_all();
    });






})();
