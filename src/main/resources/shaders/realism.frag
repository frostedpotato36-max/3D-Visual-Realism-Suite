#version 330 core
in vec3 FragPos; in vec3 Normal; in vec2 TexCoords; in vec4 FragPosLightSpace;
out vec4 FragColor;
uniform vec3 lightPos; uniform vec3 viewPos; uniform vec3 objectColor;
uniform sampler2D diffuseTexture; uniform sampler2D shadowMap;
uniform int useBlinnPhong; uniform int smoothShading; uniform int useTexture; uniform int useShadows;
float shadowCalculation(vec4 lightPos4, vec3 n, vec3 lightDir){
    vec3 proj=lightPos4.xyz/lightPos4.w; proj=proj*0.5+0.5;
    if(proj.z>1.0) return 0.0;
    float bias=max(0.003*(1.0-dot(n,lightDir)),0.0006);
    vec2 texel=1.0/textureSize(shadowMap,0); float shadow=0.0;
    for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++){
        float closest=texture(shadowMap,proj.xy+vec2(x,y)*texel).r;
        shadow += proj.z-bias>closest?1.0:0.0;
    }
    return shadow/9.0;
}
void main(){
    vec3 norm=smoothShading==1?normalize(Normal):normalize(cross(dFdx(FragPos),dFdy(FragPos)));
    vec3 lightDir=normalize(lightPos-FragPos); vec3 viewDir=normalize(viewPos-FragPos);
    float diff=max(dot(norm,lightDir),0.0);
    float spec=0.0;
    if(useBlinnPhong==1){vec3 halfwayDir=normalize(lightDir+viewDir);spec=pow(max(dot(norm,halfwayDir),0.0),32.0);}
    else{vec3 reflectDir=reflect(-lightDir,norm);spec=pow(max(dot(viewDir,reflectDir),0.0),16.0);}
    vec3 base=useTexture==1?texture(diffuseTexture,TexCoords).rgb:objectColor;
    float shadow=useShadows==1?shadowCalculation(FragPosLightSpace,norm,lightDir):0.0;
    vec3 ambient=0.22*base; vec3 lighting=ambient+(1.0-shadow)*(diff*base+0.45*spec*vec3(1.0));
    FragColor=vec4(lighting,1.0);
}
