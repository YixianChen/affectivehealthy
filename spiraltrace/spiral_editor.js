// enclose in anonymous function
if(window.addEventListener) {
    // on load
    window.addEventListener('load', function () {
        // canvas, graphics context and image
        var canvas, ctx, img;
        // result area
        var resultArea;
        // point to draw
        var pointerPosition = { x: 0, y: 0, r: 30 };
        // dimensionX, dimensionY
        var dimX, dimY;
        // center
        var center = { x: 0, y: 0}
        // next point calculated by the cursor and the constrints
        var nextPoint = { x: 0, y: 0 };
        // array of points
        var vertices = new Array();
        // stats for restrictions
        var arcLength = 10; // 20
        // mode
        var mode = 0;
        // scale of the ball
        var thickness = 20;

        function mDown(ev) {
            // store starting point
            if(vertices.length == 0) {
                vertices.push({ x : ev.offsetX, y : ev.offsetY , r : 0 });
            } else { // store a vertex
                //if(mode == 0) {
                    vertices.push({ x : nextPoint.x, y : nextPoint.y, r : 0 });
                    //mode = 1;
                //} else if(mode == 1) {
                    var last = vertices[vertices.length - 1];
                    var dir = { x : pointerPosition.x - last.x,
                        y : pointerPosition.y - last.y };
                    var r = thickness;//Math.sqrt(dir.x * dir.x + dir.y * dir.y);
                    vertices[vertices.length - 1].r = r;
                    //mode = 0;    
                //}
            }
            draw();
            return true;
        }

        function mUp(ev) {
        }

        function mMove(ev) {
            pointerPosition.x = ev.offsetX;
            pointerPosition.y = ev.offsetY;
            draw();
        }

        function save() {
            resultArea.value = '';
            var scale = 1.0 / Math.min(dimX, dimY);
			var xscale = 1.0 / dimX;
			var yscale = 1.0 / dimY;
            console.log('saving values');
            //console.log(vertices.length);
            for(var i = 0; i < vertices.length; i++) {
                resultArea.value += ((vertices[i].x - center.x) * xscale) + ' ' + ((vertices[i].y - center.y) * yscale) + ' ' + (vertices[i].r * scale) + '\n';
            }
            //console.log('----------');
        }

        function kDown(ev) {
            switch(ev.keyCode) {
                case 81:
                    thickness += 0.1;
                    draw();
                break;
                case 65:
                    thickness -= 0.1;
                    draw();
                break;
                case 68:
                    vertices = new Array();
                    draw();
                break;
                case 90:
                    vertices.pop();
                    draw();
                break;
                case 83:
                    save();
                break;
                default:
                console.log('Pressed: ' + ev.keyCode);
            }
        }

        // init application
        function init() {
            // get canvas, assume succes
            canvas = document.getElementById("canvas");
            // get canvas, assume succes
            canvasZoom = document.getElementById("canvasZoom");
            // get resultArea
            resultArea = document.getElementById("resultArea");
            // dimension
            dimX = canvas.width;
            dimY = canvas.height;
            center.x = dimX / 2;
            center.y = dimY / 2;
            // get context, assume succes
            ctx = canvas.getContext("2d");
            // get context, assume succes
            ctxZoom = canvasZoom.getContext("2d");
            // event listeners
            canvas.addEventListener('mousedown', mDown, false);
            canvas.addEventListener('mousemove', mMove, false);
            canvas.addEventListener('mouseup',   mUp,   false);
            window.addEventListener('keydown',   kDown, false);
            img = new Image();
            img.onload = function(){  
              draw();
            };  
            img.src = 'background_stencil3.png';  
        }

        function updateNextPoint() {
            var last = vertices[vertices.length - 1];
            var dir = { x : pointerPosition.x - last.x,
                y : pointerPosition.y - last.y };
            var l = 1.0 / Math.sqrt(dir.x * dir.x + dir.y * dir.y);
            var ndir = { x : dir.x * l, y : dir.y * l };

            nextPoint.x = last.x + ndir.x * arcLength;
            nextPoint.y = last.y + ndir.y * arcLength; 
            nextPoint.r = thickness;
        }

        function drawRadiusMarker() {
            var last = vertices[vertices.length - 1];
            var dir = { x : pointerPosition.x - last.x,
                y : pointerPosition.y - last.y };
            var r = thickness;//Math.sqrt(dir.x * dir.x + dir.y * dir.y);

            ctx.fillStyle = "rgba(10, 20, 30, 0.5)";
            ctx.beginPath();
            ctx.arc(last.x, last.y, r, 0, Math.PI*2, true);
            ctx.closePath();
            ctx.fill();
        }

        function drawRadius(point, opacity) {
			if (!opacity) opacity = 0.1;
            ctx.fillStyle = "rgba(10, 20, 30, 0.25)";
            ctx.beginPath();
            ctx.arc(point.x, point.y, point.r, 0, Math.PI*2, true);
            ctx.closePath();
            ctx.fill();
        }

        function zoom(point) {
            ctxZoom.drawImage(canvas, point.x - 50, point.y - 50, 100, 100, 0, 0, 200, 200);
        }

        function draw() {  
            // clear
            ctx.drawImage(img, 0, 0);
            
            if(vertices.length > 0) {
                ctx.strokeStyle = "rgba(10, 20, 30, 0.5)";
                ctx.beginPath();
                var start = vertices[0];
                ctx.moveTo(start.x, start.y); 
                for(var i = 1; i < vertices.length; i++) {
                    ctx.lineTo(vertices[i].x, vertices[i].y);
                }
                updateNextPoint();
                if(mode == 0) {
                    ctx.lineTo(nextPoint.x, nextPoint.y);
                }
                ctx.stroke();

                for(var i = 1; i < vertices.length; i++) {
//                    drawRadius(vertices[i],0.05);
                }

                //if(mode == 1) {
                    drawRadius(nextPoint,0.25);
                //}
            } else {
				drawRadius(pointerPosition,0.25);
			}

            zoom(nextPoint);
        }

        // initialize the application
        init();
    }, false);
}