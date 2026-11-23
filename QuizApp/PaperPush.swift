import SwiftUI

/// 全局纸张推页容器。
/// 底栏页面把要进入的二级页放进 `detail`，只维护一个可选路由即可；
/// 进入/返回、根页退让和左缘拖回都由这一层统一处理。
struct PaperPushModifier<Detail: View>: ViewModifier {
    @Binding var isPresented: Bool
    let detail: () -> Detail

    @State private var isDetailVisible = false
    @State private var progress: CGFloat = 0
    @State private var dragOffset: CGFloat = 0

    private let enterDuration: Double = 0.34
    private let exitDuration: Double = 0.28

    func body(content: Content) -> some View {
        GeometryReader { geo in
            let w = max(geo.size.width, 1)
            let dragFraction = min(max(dragOffset / w, 0), 1)
            let effective = progress * (1 - dragFraction)

            ZStack {
                PaperBackground()
                content
                    .scaleEffect(1 - 0.028 * effective, anchor: .leading)
                    .offset(x: -w * 0.14 * effective)
                    .opacity(1 - Double(0.10 * effective))
                    .overlay {
                        Color.black
                            .opacity(Double(0.10 * effective))
                            .ignoresSafeArea()
                            .allowsHitTesting(false)
                    }
                    .allowsHitTesting(!isDetailVisible)

                if isDetailVisible {
                    detail()
                        .frame(width: geo.size.width, height: geo.size.height)
                        .offset(x: (1 - progress) * w + dragOffset)
                        .shadow(color: Color.black.opacity(0.18), radius: 18, x: -8, y: 0)
                        .overlay(alignment: .leading) {
                            Rectangle()
                                .fill(Palette.gold.opacity(0.18))
                                .frame(width: 1)
                        }
                        .overlay(alignment: .leading) {
                            Color.clear
                                .frame(width: 26)
                                .contentShape(Rectangle())
                                .gesture(backDrag(width: w))
                        }
                        .zIndex(2)
                }
            }
            .onAppear {
                if isPresented && !isDetailVisible {
                    isDetailVisible = true
                    progress = 1
                }
            }
        }
        .onChange(of: isPresented) { presented in
            if presented {
                present()
            } else {
                close()
            }
        }
    }

    private func present() {
        if !isDetailVisible {
            isDetailVisible = true
            progress = 0
        }
        dragOffset = 0
        DispatchQueue.main.async {
            withAnimation(.timingCurve(0.22, 0.76, 0.16, 1, duration: enterDuration)) {
                progress = 1
            }
        }
    }

    private func close() {
        guard isDetailVisible else { return }
        withAnimation(.timingCurve(0.36, 0.08, 0.18, 1, duration: exitDuration)) {
            progress = 0
            dragOffset = 0
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + exitDuration + 0.02) {
            guard !isPresented else { return }
            isDetailVisible = false
            progress = 0
            dragOffset = 0
        }
    }

    private func backDrag(width: CGFloat) -> some Gesture {
        DragGesture(minimumDistance: 12, coordinateSpace: .local)
            .onChanged { value in
                guard progress > 0.98 else { return }
                guard abs(value.translation.width) > abs(value.translation.height) else { return }
                dragOffset = min(max(value.translation.width, 0), width)
            }
            .onEnded { value in
                guard progress > 0.98 else {
                    dragOffset = 0
                    return
                }
                let dx = min(max(value.translation.width, 0), width)
                let velocity = value.predictedEndTranslation.width
                if dx > width * 0.28 || velocity > 320 {
                    isPresented = false
                } else {
                    withAnimation(.timingCurve(0.36, 0.08, 0.18, 1, duration: exitDuration)) {
                        dragOffset = 0
                    }
                }
            }
    }
}

extension View {
    func paperPush<Detail: View>(
        isPresented: Binding<Bool>,
        @ViewBuilder detail: @escaping () -> Detail
    ) -> some View {
        modifier(PaperPushModifier(isPresented: isPresented, detail: detail))
    }
}
