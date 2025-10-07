interface FrameData {
    imageData: string;
    fps: number;
    resolution: string;
    timestamp: number;
    processingTime: number;
}

class EdgeDetectionViewer {
    // REFACTOR: Added definite assignment assertion (!) as they are handled in initializeElements
    private canvas!: HTMLCanvasElement;
    private ctx!: CanvasRenderingContext2D;
    private fpsElement!: HTMLElement;
    private resolutionElement!: HTMLElement;
    private statusElement!: HTMLElement;

    private currentFrame: FrameData | null = null;
    private isEdgeDetectionMode: boolean = true;

    // REFACTOR: Create a single image object to reuse for rendering
    private frameImage: HTMLImageElement = new Image();

    // REFACTOR: Moved FPS calculation logic into the main render loop
    private frameCounter: number = 0;
    private lastFpsUpdate: number = 0;

    constructor() {
        this.initializeElements();
        this.setupEventListeners();

        // REFACTOR: Set up the image's onload once to avoid re-assigning it
        this.frameImage.onload = () => {
            this.renderFrame();
        };

        this.loadSampleFrame();
        this.startRenderLoop();
    }

    private initializeElements(): void {
        // REFACTOR: Added helper function for robust element fetching
        const getElement = <T extends HTMLElement>(id: string): T => {
            const element = document.getElementById(id);
            if (!element) {
                throw new Error(`Fatal: Element with ID '${id}' was not found in the DOM.`);
            }
            return element as T;
        };

        this.canvas = getElement<HTMLCanvasElement>('frameCanvas');
        this.ctx = this.canvas.getContext('2d')!;
        this.fpsElement = getElement<HTMLElement>('fpsCounter');
        this.resolutionElement = getElement<HTMLElement>('resolutionInfo');
        this.statusElement = getElement<HTMLElement>('statusInfo');
    }

    private setupEventListeners(): void {
        const modeToggle = document.getElementById('modeToggle') as HTMLButtonElement;
        const loadSampleBtn = document.getElementById('loadSampleBtn') as HTMLButtonElement;

        modeToggle.addEventListener('click', () => this.toggleDisplayMode());
        loadSampleBtn.addEventListener('click', () => this.loadSampleFrame());
    }

    private loadSampleFrame(): void {
        try {
            this.currentFrame = {
                imageData: this.generateSampleFrame(),
                fps: 30.0, // Source FPS
                resolution: "640x480",
                timestamp: Date.now(),
                processingTime: 45.2
            };
            
            // REFACTOR: Update the single image source. The onload handler will trigger rendering.
            this.frameImage.src = this.currentFrame.imageData;
            this.updateStats(); // Update stats immediately
            this.updateStatus("Sample frame loaded successfully");
        } catch (error) {
            this.updateStatus("Error loading sample frame: " + error);
        }
    }

    private generateSampleFrame(): string {
        const tempCanvas = document.createElement('canvas');
        tempCanvas.width = 640;
        tempCanvas.height = 480;
        const tempCtx = tempCanvas.getContext('2d')!;
        const gradient = tempCtx.createLinearGradient(0, 0, 640, 480);
        gradient.addColorStop(0, '#1a1a2e');
        gradient.addColorStop(1, '#16213e');
        tempCtx.fillStyle = gradient;
        tempCtx.fillRect(0, 0, 640, 480);
        tempCtx.strokeStyle = '#00b4d8';
        tempCtx.lineWidth = 2;
        for (let i = 0; i < 20; i++) {
            const x = Math.random() * 640;
            const y = Math.random() * 480;
            const length = 50 + Math.random() * 100;
            tempCtx.beginPath();
            tempCtx.moveTo(x, y);
            for (let j = 0; j < 5; j++) {
                const nextX = x + (Math.random() - 0.5) * length;
                const nextY = y + (Math.random() - 0.5) * length;
                tempCtx.lineTo(nextX, nextY);
            }
            tempCtx.stroke();
        }
        tempCtx.fillStyle = '#e63946';
        for (let i = 0; i < 100; i++) {
            const x = Math.random() * 640;
            const y = Math.random() * 480;
            tempCtx.fillRect(x, y, 2, 2);
        }
        return tempCanvas.toDataURL();
    }
    
    // REFACTOR: This new method contains only the drawing logic
    private renderFrame(): void {
        if (!this.currentFrame) return;

        // Clear canvas
        this.ctx.fillStyle = '#000000';
        this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

        // Calculate aspect ratio and draw image centered
        const img = this.frameImage;
        const scale = Math.min(this.canvas.width / img.width, this.canvas.height / img.height);
        const width = img.width * scale;
        const height = img.height * scale;
        const x = (this.canvas.width - width) / 2;
        const y = (this.canvas.height - height) / 2;
        
        this.ctx.drawImage(img, x, y, width, height);

        // Draw overlay only if in the correct mode
        if (this.isEdgeDetectionMode) {
            this.drawOverlay();
        }
    }

    private drawOverlay(): void {
        if (!this.currentFrame) return;

        this.ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
        this.ctx.fillRect(10, 10, 200, 80);
        this.ctx.fillStyle = '#ffffff';
        this.ctx.font = '12px monospace';
        
        const renderFps = this.frameCounter; // Use the current render FPS
        this.ctx.fillText(`Render FPS: ${renderFps.toFixed(1)}`, 20, 30);
        this.ctx.fillText(`Source Res: ${this.currentFrame.resolution}`, 20, 50);
        this.ctx.fillText(`Processing: ${this.currentFrame.processingTime.toFixed(1)}ms`, 20, 70);
    }
    
    private updateStats(): void {
        if (!this.currentFrame) return;

        // Display the rendering FPS and source resolution
        this.fpsElement.textContent = this.frameCounter.toFixed(1);
        this.resolutionElement.textContent = this.currentFrame.resolution;
    }

    private updateStatus(message: string): void {
        this.statusElement.textContent = message;
        setTimeout(() => {
            if (this.statusElement.textContent === message) {
                this.statusElement.textContent = 'Ready';
            }
        }, 3000);
    }

    private toggleDisplayMode(): void {
        this.isEdgeDetectionMode = !this.isEdgeDetectionMode;
        const modeToggle = document.getElementById('modeToggle') as HTMLButtonElement;
        
        if (this.isEdgeDetectionMode) {
            modeToggle.textContent = 'Show Raw Feed';
            this.updateStatus('Display mode: Edge Detection');
        } else {
            modeToggle.textContent = 'Show Edge Detection';
            this.updateStatus('Display mode: Raw Feed');
        }

        // REFACTOR: Force a re-render to show/hide the overlay
        this.renderFrame();
    }

    private startRenderLoop(): void {
        const render = (timestamp: number) => {
            this.frameCounter++;
            
            // REFACTOR: Merged FPS calculation into the render loop
            if (timestamp - this.lastFpsUpdate >= 1000) {
                this.updateStats();
                this.frameCounter = 0;
                this.lastFpsUpdate = timestamp;
            }

            // The rendering itself is now handled by the image.onload event
            // to prevent re-drawing the same image 60 times a second.
            // We only need the loop for FPS counting.
            
            requestAnimationFrame(render);
        };
        requestAnimationFrame(render);
    }
    
    public updateFrameData(frameData: FrameData): void {
        this.currentFrame = frameData;
        // REFACTOR: Just update the image source. `onload` will handle the rest.
        this.frameImage.src = this.currentFrame.imageData;
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new EdgeDetectionViewer();
});

export default EdgeDetectionViewer;